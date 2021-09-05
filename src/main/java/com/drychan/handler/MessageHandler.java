package com.drychan.handler;

import com.drychan.client.VkApiClientWrapper;
import com.drychan.dao.model.Like;
import com.drychan.dao.model.User;
import com.drychan.model.ObjectMessage;
import com.drychan.service.LikeService;
import com.drychan.service.UserService;
import com.drychan.transformer.PhotoTransformer;
import com.drychan.utils.PhotoUtils;
import com.vk.api.sdk.client.actors.GroupActor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
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

    private final MessageSender messageSender;

    private final UserService userService;

    private final LikeService likeService;

    private final PhotoUtils photoUtils;

    private final int SEEN_CACHE_SIZE = 10000;

    //todo: setup redis
    private final LinkedHashMap<Integer, Integer> lastSeenProfile;

    public static final String NEXT_LINE = System.lineSeparator();

    public MessageHandler(@Value("${vk.token}") String token,
                          @Value("${group.id}") String groupIdAsString,
                          UserService userService,
                          LikeService likeService,
                          PhotoTransformer photoTransformer) {
        this.userService = userService;
        this.likeService = likeService;
        GroupActor actor = new GroupActor(Integer.parseInt(groupIdAsString), token);
        VkApiClientWrapper apiClient = new VkApiClientWrapper();
        this.photoUtils = new PhotoUtils(actor, apiClient, photoTransformer);
        messageSender = new MessageSender(actor, apiClient);
        lastSeenProfile = new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, Integer> eldest) {
                return size() > SEEN_CACHE_SIZE;
            }
        };
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
            messageSender.send(userId, HELP_MESSAGE, null, helpKeyboard(true));
            messageSender.send(userId, "Как тебя зовут?)");
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
                photoUtils);
        if (isProcessed && draftUserProcessingStage == DraftUserProcessingStage.NO_PHOTO_PATH) {
            suggestProfile(user.getGender(), user.getUserId());
        }
    }

    private void processPublishedUser(User user, ObjectMessage message) {
        int userId = user.getUserId();
        String messageText = message.getText();
        Integer lastSeenId = lastSeenProfile.get(userId);
        if (lastSeenId == null) {
            messageSender.send(userId, "Прошло слишком много времени");
            suggestProfile(user.getGender(), userId);
        } else {
            if (messageText.equals(LIKE)) {
                likeService.putLike(new Like(userId, lastSeenId));
                if (likeService.isLikeExists(new Like(lastSeenId, userId))) {
                    Optional<User> lastSeenUser = userService.findById(lastSeenId);
                    if (lastSeenUser.isEmpty()) {
                        messageSender.send(userId, "Пара вас лайкнула, но уже удалилась из приложения");
                        return;
                    }
                    messageSender.send(userId, "match with https://vk.com/id".concat(String.valueOf(lastSeenId)),
                            lastSeenUser.get().getPhotoPath(), null);
                    messageSender.send(lastSeenId, "match with https://vk.com/id".concat(String.valueOf(userId)),
                            user.getPhotoPath(), null);
                }
                suggestProfile(user.getGender(), userId);
            } else if (messageText.equals(DISLIKE)) {
                suggestProfile(user.getGender(), userId);
            } else {
                messageSender.send(userId, "Ответ должен быть в формате " + LIKE + "/" + DISLIKE +
                        ", наберите help, чтобы получить список команд");
            }
        }
    }

    private void suggestProfile(char gender, int userId) {
        char searchGender = gender == 'm' ? 'f' : 'm';
        Integer foundId = userService.findRandomNotLikedByUserWithGender(userId, searchGender);
        if (foundId == null) {
            messageSender.send(userId, "Вы лайкнули всех людей!");
        } else {
            var maybeFoundUser = userService.findById(foundId);
            assert maybeFoundUser.isPresent() : "user_id exists in likes db, but doesnt exist in users";
            User foundUser = maybeFoundUser.get();
            messageSender.send(userId, foundUser.getName() + ", " + foundUser.getAge() +
                    NEXT_LINE + foundUser.getDescription(), foundUser.getPhotoPath(), likeNoKeyboard(true));
            lastSeenProfile.put(userId, foundUser.getUserId());
        }
    }

}
