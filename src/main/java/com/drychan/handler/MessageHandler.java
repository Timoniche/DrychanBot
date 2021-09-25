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

import static com.drychan.handler.DefaultCommands.HELP_MESSAGE;
import static com.drychan.handler.DefaultCommands.getCommandFromText;
import static com.drychan.handler.DefaultCommands.helpKeyboard;
import static com.drychan.model.Keyboard.DISLIKE;
import static com.drychan.model.Keyboard.LIKE;
import static com.drychan.model.Keyboard.likeNoKeyboard;

@Component
@Log4j2
public class MessageHandler {
    private final static String HTTP_ID_PREFIX = "https://vk.com/id";

    private final MessageSender messageSender;

    private final UserService userService;

    private final LikeService likeService;

    private final SuggestedService suggestedService;

    private final PhotoUtils photoUtils;

    private final AudioUtils audioUtils;

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
        GroupActor actor = new GroupActor(Integer.parseInt(groupIdAsString), token);
        VkApiClientWrapper apiClient = new VkApiClientWrapper();
        this.photoUtils = new PhotoUtils(actor, apiClient, photoTransformer);
        this.audioUtils = new AudioUtils(actor, apiClient, audioMessageTransformer);
        messageSender = new MessageSender(actor, apiClient);
    }

    public void handleMessage(ObjectMessage message) {
        int userId = message.getUserId();
        String messageText = message.getText();
        log.info("user_id={} sent message={}", message.getUserId(), message.getText());
        DefaultCommands maybeDefaultCommand = getCommandFromText(messageText);
        if (maybeDefaultCommand != null) {
            maybeDefaultCommand.processCommand(userId, messageSender, userService, likeService);
            return;
        }
        var maybeUser = userService.findById(userId);
        if (maybeUser.isEmpty()) {
            var user = User.builder()
                    .userId(userId)
                    .status(User.Status.draft)
                    .build();
            userService.saveUser(user);
            log.info("user_id={} saved to draft", userId);
            messageSender.send(MessageSender.MessageSendQuery.builder()
                    .userId(userId)
                    .message(HELP_MESSAGE)
                    .keyboard(helpKeyboard(true))
                    .build());
            messageSender.send(MessageSender.MessageSendQuery.builder()
                    .userId(userId)
                    .message("Как тебя зовут?)")
                    .build());
        } else {
            User user = maybeUser.get();
            if (user.getStatus() == User.Status.draft) {
                processDraftUser(user, message);
            } else {
                processPublishedUser(user, message);
            }
        }
    }

    private void processDraftUser(User user, ObjectMessage message) {
        DraftUserProcessingStage draftUserProcessingStage = DraftUserProcessingStage.getStageFromUser(user);
        if (draftUserProcessingStage == null) {
            log.warn("processDraftUser for published user with id {}", user.getUserId());
            return;
        }
        boolean isProcessed = draftUserProcessingStage.processUserStage(user, messageSender, userService, message,
                photoUtils, audioUtils);
        if (isProcessed && draftUserProcessingStage == DraftUserProcessingStage.NO_VOICE_ATTACHMENT) { //todo: change logic to verify profile
            suggestProfile(user.getGender(), user.getUserId());
        }
    }

    private void processPublishedUser(User user, ObjectMessage message) {
        int userId = user.getUserId();
        String messageText = message.getText();
        Integer lastSeenId = suggestedService.lastSuggestedUserId(userId);
        if (messageText.equals(LIKE)) {
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
        } else if (messageText.equals(DISLIKE)) {
            suggestProfile(user.getGender(), userId);
        } else {
            messageSender.send(MessageSender.MessageSendQuery.builder()
                    .userId(userId)
                    .message("Ответ должен быть в формате " + LIKE + "/" + DISLIKE +
                            ", наберите help, чтобы получить список команд")
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

    private void suggestProfile(char gender, int userId) {
        char searchGender = gender == 'm' ? 'f' : 'm';
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
