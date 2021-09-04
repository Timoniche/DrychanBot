package com.drychan.handler;

import com.drychan.dao.model.Like;
import com.drychan.dao.model.User;
import com.drychan.model.MessageNewObject;
import com.drychan.service.LikeService;
import com.drychan.service.UserService;
import com.drychan.transformer.PhotoTransformer;
import com.drychan.utils.PhotoUtils;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Log4j2
public class MessageHandler {
    private final GroupActor actor;

    private final VkApiClient apiClient;

    private final MessageSender messageSender;

    private final UserService userService;

    private final LikeService likeService;

    private final PhotoUtils photoUtils;

    private final int SEEN_CACHE_SIZE = 10000;

    //todo: setup redis
    private final LinkedHashMap<Integer, Integer> lastSeenProfile;

    private static final String NEXT_LINE = System.lineSeparator();

    public static final String HELP_MESSAGE = "Список команд:" + NEXT_LINE +
            "help : вывести список команд" + NEXT_LINE +
            "delete : удалить свой аккаунт";

    public MessageHandler(@Value("${vk.token}") String token,
                          @Value("${group.id}") String groupIdAsString,
                          UserService userService,
                          LikeService likeService,
                          PhotoTransformer photoTransformer) {
        HttpTransportClient client = new HttpTransportClient();
        apiClient = new VkApiClient(client);
        actor = new GroupActor(Integer.parseInt(groupIdAsString), token);
        this.userService = userService;
        this.likeService = likeService;
        this.photoUtils = new PhotoUtils(actor, apiClient, photoTransformer);
        messageSender = new MessageSender(actor, apiClient);
        lastSeenProfile = new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, Integer> eldest) {
                return size() > SEEN_CACHE_SIZE;
            }
        };
    }

    public void handleMessage(MessageNewObject message) {
        int userId = message.getUserId();
        String messageText = message.getBody();
        log.info("user_id={} sent message={}", message.getUserId(), message.getBody());
        if (messageText.equals("help")) {
            messageSender.send(userId, HELP_MESSAGE);
            return;
        }
        if (messageText.equals("delete")) {
            messageSender.send(userId, "Ваш профиль удален, напишите start, чтобы создать новую анкету");
            userService.deleteById(userId);
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
            messageSender.send(userId, HELP_MESSAGE);
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

    private void processDraftUser(User user, MessageNewObject message) {
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

    private void processPublishedUser(User user, MessageNewObject message) {
        int userId = user.getUserId();
        String messageText = message.getBody();
        Integer lastSeenId = lastSeenProfile.get(userId);
        if (lastSeenId == null) {
            messageSender.send(userId, "Прошло слишком много времени");
            suggestProfile(user.getGender(), userId);
        } else {
            if (messageText.equals("like")) {
                likeService.putLike(new Like(userId, lastSeenId));
                if (likeService.isLikeExists(new Like(lastSeenId, userId))) {
                    Optional<User> lastSeenUser = userService.findById(lastSeenId);
                    if (lastSeenUser.isEmpty()) {
                        messageSender.send(userId, "Пара вас лайкнула, но уже удалилась из приложения");
                        return;
                    }
                    messageSender.send(userId, "match with https://vk.com/id".concat(String.valueOf(lastSeenId)),
                            lastSeenUser.get().getPhotoPath());
                    messageSender.send(lastSeenId, "match with https://vk.com/id".concat(String.valueOf(userId)),
                            user.getPhotoPath());
                }
                suggestProfile(user.getGender(), userId);
            } else if (messageText.equals("no")) {
                suggestProfile(user.getGender(), userId);
            } else {
                messageSender.send(userId, "Ответ должен быть в формате like/no, " +
                        "наберите help, чтобы получить список команд");
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
                    NEXT_LINE + foundUser.getDescription() +
                    NEXT_LINE + " [like/no]", foundUser.getPhotoPath());
            lastSeenProfile.put(userId, foundUser.getUserId());
        }
    }

}
