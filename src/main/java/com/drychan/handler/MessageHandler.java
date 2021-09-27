package com.drychan.handler;

import com.drychan.client.VkApiClientWrapper;
import com.drychan.dao.model.Like;
import com.drychan.dao.model.User;
import com.drychan.model.ObjectMessage;
import com.drychan.service.LikeService;
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

import java.util.Optional;

import static com.drychan.dao.model.User.Gender.FEMALE;
import static com.drychan.dao.model.User.Gender.MALE;
import static com.drychan.dao.model.User.Status.DRAFT;
import static com.drychan.handler.DefaultCommands.HELP;
import static com.drychan.handler.DefaultCommands.getCommandFromText;
import static com.drychan.handler.DraftUserProcessor.DraftStage;
import static com.drychan.handler.DraftUserProcessor.DraftStage.WAITING_APPROVE;
import static com.drychan.handler.DraftUserProcessor.DraftStage.getStageFromUser;
import static com.drychan.model.Keyboard.DISLIKE_LABEL;
import static com.drychan.model.Keyboard.LIKE_LABEL;
import static com.drychan.model.Keyboard.helpSuggestionKeyboard;
import static com.drychan.model.Keyboard.likeNoKeyboard;
import static com.drychan.dao.model.User.Gender;

@Component
@Log4j2
public class MessageHandler {
    private final static String HTTP_ID_PREFIX = "https://vk.com/id";

    private final MessageSender messageSender;

    private final UserService userService;

    private final LikeService likeService;

    private final SuggestedService suggestedService;

    private final DraftUserProcessor draftUserProcessor;

    public static final String NEXT_LINE = System.lineSeparator();

    public MessageHandler(@Value("${vk.token}") String token,
                          @Value("${group.id}") String groupIdAsString,
                          UserService userService,
                          LikeService likeService,
                          SuggestedService suggestedService,
                          PhotoTransformer photoTransformer,
                          AudioMessageTransformer audioMessageTransformer) {
        this.userService = userService;
        this.likeService = likeService;
        this.suggestedService = suggestedService;
        GroupActor groupActor = new GroupActor(Integer.parseInt(groupIdAsString), token);
        VkApiClientWrapper apiClient = new VkApiClientWrapper();
        messageSender = new MessageSender(groupActor, apiClient);
        PhotoUtils photoUtils = new PhotoUtils(groupActor, apiClient, photoTransformer);
        AudioUtils audioUtils = new AudioUtils(groupActor, apiClient, audioMessageTransformer);
        this.draftUserProcessor = new DraftUserProcessor(messageSender, userService, photoUtils, audioUtils,
                apiClient, groupActor);
    }

    public void handleMessage(ObjectMessage message) {
        int userId = message.getUserId();
        String messageText = message.getText();
        log.info("user_id={} sent message={}", message.getUserId(), message.getText());
        DefaultCommands maybeDefaultCommand = getCommandFromText(messageText);
        if (maybeDefaultCommand != null) {
            maybeDefaultCommand.processCommand(userId, messageSender, userService, likeService, draftUserProcessor);
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
        Integer lastSeenId = suggestedService.lastSuggestedUserId(userId);
        if (messageText.equals(LIKE_LABEL)) {
            likeService.putLike(new Like(userId, lastSeenId));
            if (likeService.isLikeExists(new Like(lastSeenId, userId))) {
                Optional<User> lastSeenUser = userService.findById(lastSeenId);
                if (lastSeenUser.isEmpty()) {
                    messageSender.send(MessageSender.MessageSendQuery.builder()
                            .userId(userId)
                            .message("Пара вас лайкнула, но уже удалилась из приложения")
                            .build());
                    return;
                }
                matchProcessing(user, lastSeenUser.get());
            }
            suggestProfile(user.getGender(), userId);
        } else if (messageText.equals(DISLIKE_LABEL)) {
            suggestProfile(user.getGender(), userId);
        } else {
            messageSender.send(MessageSender.MessageSendQuery.builder()
                    .userId(userId)
                    .message("Ответ должен быть в формате " + LIKE_LABEL + "/" + DISLIKE_LABEL +
                            ", нажми " + HELP.getCommand() + ", чтобы получить список команд")
                    .keyboard(helpSuggestionKeyboard(true))
                    .build());
        }
    }

    private void matchProcessing(User userFst, User userSnd) {
        String userFstUri = HTTP_ID_PREFIX + userFst.getUserId();
        String userSndUri = HTTP_ID_PREFIX + userSnd.getUserId();

        messageSender.send(MessageSender.MessageSendQuery.builder()
                .userId(userFst.getUserId())
                .message(userSnd.getName() + " ответил" + (userSnd.isFemale() ? "а" : "")
                        + " взаимностью!"
                        + NEXT_LINE
                        + userSndUri)
                .photoAttachmentPath(userSnd.getPhotoPath())
                .build());
        messageSender.send(MessageSender.MessageSendQuery.builder()
                .userId(userSnd.getUserId())
                .message(userFst.getName() + " ответил" + (userFst.isFemale() ? "а" : "")
                        + " взаимностью!"
                        + NEXT_LINE
                        + userFstUri)
                .photoAttachmentPath(userFst.getPhotoPath())
                .build());
    }

    private void suggestProfile(Gender gender, int userId) {
        Gender searchGender = gender == MALE ? FEMALE : MALE;
        Integer foundId = userService.findRandomNotLikedByUserWithGender(userId, searchGender);
        if (foundId == null) {
            messageSender.send(MessageSender.MessageSendQuery.builder()
                    .userId(userId)
                    .message("Вы лайкнули всех людей!")
                    .build());
        } else {
            var maybeFoundUser = userService.findById(foundId);
            assert maybeFoundUser.isPresent() : "user_id exists in likes db, but doesn't exist in users";
            User foundUser = maybeFoundUser.get();
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

}
