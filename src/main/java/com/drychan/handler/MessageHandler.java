package com.drychan.handler;

import com.drychan.model.Like;
import com.drychan.model.User;
import com.drychan.service.LikeService;
import com.drychan.service.UserService;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import lombok.extern.log4j.Log4j2;
import org.postgresql.translation.messages_bg;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

@Component
@Log4j2
public class MessageHandler {
    private final GroupActor actor;

    private final VkApiClient apiClient;

    private final Random random;

    private final UserService userService;

    private final LikeService likeService;

    private final int SEEN_CACHE_SIZE = 10000;

    private final LinkedHashMap<Integer, Integer> lastSeenProfile;

    public MessageHandler(@Value("${vk.token}") String token,
                          @Value("${group.id}") String groupIdAsString,
                          UserService userService,
                          LikeService likeService) {
        HttpTransportClient client = new HttpTransportClient();
        apiClient = new VkApiClient(client);
        actor = new GroupActor(Integer.parseInt(groupIdAsString), token);
        this.userService = userService;
        this.likeService = likeService;
        random = new Random();
        lastSeenProfile = new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, Integer> eldest) {
                return size() > SEEN_CACHE_SIZE;
            }
        };
    }

    public void handleMessage(int userId, String message) {
        log.info("user_id={} sent message={}", userId, message);
        var maybeUser = userService.findById(userId);
        if (maybeUser.isEmpty()) {
            var user = User.builder()
                    .userId(userId)
                    .status(User.Status.draft)
                    .build();
            userService.saveUser(user);
            log.info("user_id={} saved to draft", userId);
            sendMessage(userId, "Как тебя зовут?)");
        } else {
            var user = maybeUser.get();
            if (user.getStatus() == User.Status.draft) {
                processDraftUser(user, userId, message);
            } else {
                processPublishedUser(user, userId, message);
            }
        }
    }

    private void processDraftUser(User user, int userId, String message) {
        if (user.getName() == null) {
            if (message.isBlank()) {
                sendMessage(userId, "Ты уверен, что твое имя на Whitespace?)");
            } else {
                user.setName(message);
                userService.saveUser(user);
                log.info("user_id={} set name to {}", userId, message);
                sendMessage(userId, "Прекрасное имя! Теперь укажи свой пол) [м/ж]");
            }
        } else if (user.getGender() == null) {
            if (!message.equals("м") && !message.equals("ж")) {
                sendMessage(userId, "Есть всего 2 гендера, м и ж, попробуй еще раз)");
            } else {
                boolean isMale = message.equals("м");
                if (isMale) {
                    user.setGender('m');
                } else {
                    user.setGender('f');
                }
                userService.saveUser(user);
                log.info("user_id={} set gender to {}", userId, message);
                String genderDependentQuestion;
                if (isMale) {
                    genderDependentQuestion = "Сколько тебе лет, парень? Надеюсь, ты пришел не пикапить школьниц)";
                } else {
                    genderDependentQuestion = "У девушки, конечно, невежливо спрашивать возраст, но я рискну)";
                }
                sendMessage(userId, genderDependentQuestion);
            }
        } else if (user.getAge() == null) {
            try {
                int age = Integer.parseInt(message);
                user.setAge(age);
                userService.saveUser(user);
                log.info("user_id={} set age to {}", userId, age);
                sendMessage(userId, "Осталось только придумать остроумное описание!");
            } catch (NumberFormatException ex) {
                sendMessage(userId, "Столько не живут)");
            }
        } else {
            if (message.isBlank()) {
                sendMessage(userId, "Хм, немногословно) Попробуй еще раз!");
            } else {
                user.setDescription(message);
                user.setStatus(User.Status.published);
                userService.saveUser(user);
                log.info("user_id={} set description to {}", userId, message);
                log.info("user_id={} is published", userId);
                suggestProfile(user.getGender(), userId);
            }
        }
    }

    private void processPublishedUser(User user, int userId, String message) {
        Integer lastSeenId = lastSeenProfile.get(userId);
        if (lastSeenId == null) {
            sendMessage(userId, "Прошло слишком много времени");
        } else {
            if (message.equals("like")) {
                likeService.putLike(new Like(userId, lastSeenId));
                if (likeService.isLikeExists(new Like(lastSeenId, userId))) {
                    sendMessage(userId, "match with ".concat(String.valueOf(lastSeenId)));
                    sendMessage(lastSeenId, "match with ".concat(String.valueOf(userId)));
                }
            }
        }
        suggestProfile(user.getGender(), userId);
    }

    private void suggestProfile(char gender, int userId) {
        char searchGender = gender == 'm' ? 'f' : 'm';
        Integer foundId = userService.findRandomNotLikedByUserWithGender(userId, searchGender);
        if (foundId == null) {
            sendMessage(userId, "Вы лайкнули всех людей!");
        } else {
            var maybeFoundUser = userService.findById(foundId);
            assert maybeFoundUser.isPresent() : "user_id exists in likes db, but doesnt exist in users";
            User foundUser = maybeFoundUser.get();
            sendMessage(userId, foundUser.getName() + " " + foundUser.getAge() + " " + foundUser.getDescription() +
                    " [like/no]");
            lastSeenProfile.put(userId, foundUser.getUserId());
        }
    }

    private void sendMessage(int userId, String message) {
        try {
            if (userId < 1000) {
                log.info("message={} to fake account id={}", message, userId);
                return;
            }
            apiClient.messages()
                    .send(actor)
                    .message(message)
                    .userId(userId).randomId(random.nextInt()).execute();
            log.info("message={} sent to userId={}", message, userId);
        } catch (ApiException e) {
            log.error("INVALID REQUEST", e);
        } catch (ClientException e) {
            log.error("NETWORK ERROR", e);
        }
    }
}
