package com.drychan.handler;

import com.drychan.client.LichessClient;
import com.drychan.client.VkApiClientWrapper;
import com.drychan.client.model.chess.CreatedGame;
import com.drychan.dao.model.LastSuggestedUser;
import com.drychan.dao.model.User;
import com.drychan.dao.model.UsersRelation;
import com.drychan.dao.model.id.UsersRelationId;
import com.drychan.model.Keyboard;
import com.drychan.model.ObjectMessage;
import com.drychan.service.UsersRelationService;
import com.drychan.service.SuggestedService;
import com.drychan.service.UserService;
import com.drychan.transformer.AudioMessageTransformer;
import com.drychan.transformer.PhotoTransformer;
import com.drychan.utils.AudioUtils;
import com.drychan.utils.PhotoUtils;
import com.vk.api.sdk.client.actors.GroupActor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static com.drychan.dao.model.User.Gender.FEMALE;
import static com.drychan.dao.model.User.Gender.MALE;
import static com.drychan.dao.model.User.Status.DRAFT;
import static com.drychan.handler.DefaultCommandsProcessor.DefaultCommands;
import static com.drychan.handler.DefaultCommandsProcessor.DefaultCommands.HELP;
import static com.drychan.handler.DefaultCommandsProcessor.DefaultCommands.getDefaultCommandFromText;
import static com.drychan.handler.DraftUserProcessor.DraftStage;
import static com.drychan.handler.DraftUserProcessor.DraftStage.WAITING_APPROVE;
import static com.drychan.handler.DraftUserProcessor.DraftStage.getStageFromUser;
import static com.drychan.handler.MessageSender.MessageSendQuery;
import static com.drychan.model.Keyboard.CHECK_NEW_PROFILES_LABEL;
import static com.drychan.model.Keyboard.DISLIKE_LABEL;
import static com.drychan.model.Keyboard.LIKE_LABEL;
import static com.drychan.model.Keyboard.MIX_DISLIKED_PROFILES_LABEL;
import static com.drychan.model.Keyboard.checkNewProfilesButton;
import static com.drychan.model.Keyboard.helpSuggestionKeyboard;
import static com.drychan.model.Keyboard.keyboardFromButton;
import static com.drychan.model.Keyboard.keyboardFromTwoVerticalButtons;
import static com.drychan.model.Keyboard.likeNoKeyboard;
import static com.drychan.dao.model.User.Gender;
import static com.drychan.model.Keyboard.mixDislikedProfilesButton;

@Component
@Log4j2
public class MessageHandler {
    public static final String NEXT_LINE = System.lineSeparator();

    private final static String HTTP_ID_PREFIX = "https://vk.com/id";

    private final GroupActor groupActor;

    private final VkApiClientWrapper apiClient;

    private final MessageSender messageSender;

    private final UserService userService;

    private final UsersRelationService usersRelationService;

    private final SuggestedService suggestedService;

    private final DraftUserProcessor draftUserProcessor;

    private final DefaultCommandsProcessor defaultCommandsProcessor;

    private final LichessClient lichessClient;

    public MessageHandler(@Value("${vk.token}") String token,
                          @Value("${group.id}") String groupIdAsString,
                          UserService userService,
                          UsersRelationService usersRelationService,
                          SuggestedService suggestedService,
                          PhotoTransformer photoTransformer,
                          AudioMessageTransformer audioMessageTransformer) {
        this.userService = userService;
        this.usersRelationService = usersRelationService;
        this.suggestedService = suggestedService;
        int groupId = Integer.parseInt(groupIdAsString);
        this.groupActor = new GroupActor(groupId, token);
        this.apiClient = new VkApiClientWrapper();
        messageSender = new MessageSender(groupActor, apiClient);
        PhotoUtils photoUtils = new PhotoUtils(groupActor, apiClient, photoTransformer);
        AudioUtils audioUtils = new AudioUtils(groupActor, apiClient, audioMessageTransformer);
        this.draftUserProcessor = new DraftUserProcessor(messageSender, userService, photoUtils, audioUtils,
                apiClient, groupActor);
        this.defaultCommandsProcessor = DefaultCommandsProcessor.builder()
                .messageSender(messageSender)
                .userService(userService)
                .usersRelationService(usersRelationService)
                .draftUserProcessor(draftUserProcessor)
                .build();
        lichessClient = new LichessClient();
    }

    public void handleMessage(ObjectMessage message) {
        int userId = message.getUserId();
        String messageText = message.getText();
        log.info("user_id={} sent message={}", message.getUserId(), message.getText());
        DefaultCommands maybeDefaultCommand = getDefaultCommandFromText(messageText);
        if (maybeDefaultCommand != null) {
            defaultCommandsProcessor.process(
                    userId,
                    defaultCommandsProcessor.chooseStrategyFromCommand(maybeDefaultCommand)
            );
            return;
        }
        var maybeUser = userService.findById(userId);
        if (maybeUser.isEmpty()) {
            var user = User.builder()
                    .userId(userId)
                    .status(DRAFT)
                    .build();
            userService.saveUser(user);
            log.info("user_id={} saved to draft", userId);
            draftUserProcessor.sendNameQuestion(userId);
        } else {
            User user = maybeUser.get();
            if (user.getStatus() == DRAFT) {
                processDraftUser(user, message);
            } else {
                processPublishedUser(user, message);
            }
        }
    }

    private void processDraftUser(User user, ObjectMessage message) {
        DraftStage stage = getStageFromUser(user);
        boolean isProcessed = draftUserProcessor.processUserStage(user, message);
        if (isProcessed && stage == WAITING_APPROVE) {
            suggestProfile(user.getGender(), user.getUserId());
        }
    }

    private void processPublishedUser(User user, ObjectMessage message) {
        int userId = user.getUserId();
        String messageText = message.getText();
        LastSuggestedUser lastSuggestedUser = suggestedService.lastSuggestedUser(userId).orElse(null);
        if (lastSuggestedUser == null) {
            log.info("No suggested users for userId {} yet ", userId);
            return;
        }
        int lastSeenId = lastSuggestedUser.getSuggestedId();
        //todo: refactor
        switch (messageText) {
            case LIKE_LABEL:
                usersRelationService.putLike(userId, lastSeenId);
                if (usersRelationService.isLikeExistsById(new UsersRelationId(lastSeenId, userId))) {
                    Optional<User> lastSeenUser = userService.findById(lastSeenId);
                    if (lastSeenUser.isEmpty()) {
                        messageSender.send(MessageSender.MessageSendQuery.builder()
                                .userId(userId)
                                .message("Пара вас лайкнула, но уже удалилась из приложения")
                                .build());
                        suggestProfile(user.getGender(), userId);
                        return;
                    }
                    matchProcessing(user, lastSeenUser.get());
                }
                suggestProfile(user.getGender(), userId);
                break;
            case DISLIKE_LABEL:
                usersRelationService.putDislike(userId, lastSeenId);
                suggestProfile(user.getGender(), userId);
                break;
            case CHECK_NEW_PROFILES_LABEL:
                suggestProfile(user.getGender(), userId);
                break;
            case MIX_DISLIKED_PROFILES_LABEL:
                usersRelationService.deleteDislikeVotesByUser(userId);
                suggestProfile(user.getGender(), userId);
                break;
            default:
                messageSender.send(MessageSender.MessageSendQuery.builder()
                        .userId(userId)
                        .message("Ответ должен быть в формате " + LIKE_LABEL + "/" + DISLIKE_LABEL +
                                ", нажми на " + HELP.getCommand() + ", чтобы получить список команд")
                        .keyboard(helpSuggestionKeyboard(true))
                        .build());
                break;
        }
    }

    private void matchProcessing(User userFst, User userSnd) {
        CreatedGame chessGame = lichessClient.createGame5Plus3();
        messageSender.send(userProfileDuplicationAfterMatch(
                userFst,
                userSnd,
                chessGame == null ? null : chessGame.getUrlWhite())
        );
        messageSender.send(userProfileDuplicationAfterMatch(
                userSnd,
                userFst,
                chessGame == null ? null : chessGame.getUrlBlack())
        );
    }

    private MessageSendQuery userProfileDuplicationAfterMatch(
            User user,
            User userMatchedWith,
            String lichessUri
    ) {
        String userMatchedWithUri = HTTP_ID_PREFIX + userMatchedWith.getUserId();
        return MessageSendQuery.builder()
                .userId(user.getUserId())
                .message(userMatchedWith.getName() + " ответил" + (userMatchedWith.isFemale() ? "а" : "")
                        + " взаимностью!"
                        + NEXT_LINE + userMatchedWithUri
                        + NEXT_LINE
                        + "Не знаешь с чего начать беседу? "
                        + "Предложи сыграть в шахматы!"
                        + NEXT_LINE + lichessUri
                        + NEXT_LINE
                        + NEXT_LINE + userMatchedWith.getName() + ", " + userMatchedWith.getAge()
                        + NEXT_LINE + userMatchedWith.getDescription()
                )
                .photoAttachmentPath(userMatchedWith.getPhotoPath())
                .voicePath(userMatchedWith.getVoicePath())
                .build();
    }

    private void suggestProfile(Gender gender, int userId) {
        Gender searchGender = gender == MALE ? FEMALE : MALE;
        User foundUser = userService.findRandomNotLikedByUserWithGender(userId, searchGender).orElse(null);
        if (foundUser == null) {
            List<UsersRelation> dislikedUsers = usersRelationService.findDislikedByUsed(userId);
            Keyboard keyboard = dislikedUsers.isEmpty() ?
                    keyboardFromButton(checkNewProfilesButton, true) :
                    keyboardFromTwoVerticalButtons(true, mixDislikedProfilesButton, checkNewProfilesButton);
            messageSender.send(MessageSender.MessageSendQuery.builder()
                    .userId(userId)
                    .message("К сожалению, все анкеты просмотрены(")
                    .keyboard(keyboard)
                    .build());
        } else {
            messageSender.send(MessageSender.MessageSendQuery.builder()
                    .userId(userId)
                    .message(foundUser.getName() + ", " + foundUser.getAge() +
                            NEXT_LINE + foundUser.getDescription())
                    .photoAttachmentPath(foundUser.getPhotoPath())
                    .voicePath(foundUser.getVoicePath())
                    .keyboard(likeNoKeyboard(true))
                    .build());
            suggestedService.saveLastSuggested(userId, foundUser.getUserId());
        }
    }

    @Deprecated
    @SuppressWarnings("unused")
    private boolean isSubscriber(int userId) {
        List<Integer> membersIds = apiClient.getGroupMembers(groupActor);
        return membersIds.contains(userId);
    }
}
