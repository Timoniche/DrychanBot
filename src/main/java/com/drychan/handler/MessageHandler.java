package com.drychan.handler;

import com.drychan.model.User;
import com.drychan.service.UserService;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@Slf4j
public class MessageHandler {
    private final GroupActor actor;

    private final VkApiClient apiClient;

    private final Random random;

    private final UserService userService;

    public MessageHandler(@Value("${vk.token}") String token,
                          @Value("${group.id}") String groupIdAsString,
                          UserService userService) {
        HttpTransportClient client = new HttpTransportClient();
        apiClient = new VkApiClient(client);
        actor = new GroupActor(Integer.parseInt(groupIdAsString), token);
        this.userService = userService;
        random = new Random();
    }

    public void handleMessage(int userId, String message) {
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
            }
        }
    }

    private void processPublishedUser(User user, int userId, String message) {
        char gender = user.getGender();
        char searchGender = gender == 'm' ? 'f' : 'm';
        Integer foundId = userService.findRandomNotLikedByUserWithGender(userId, searchGender);
        if (foundId == null) {
            sendMessage(userId, "Вы лайкнули всех людей!");
        } else {
            var maybeFoundUser = userService.findById(foundId);
            assert maybeFoundUser.isPresent() : "user_id exists in likes db, but doesnt exist in users";
            User foundUser = maybeFoundUser.get();
            sendMessage(userId, foundUser.getName() + " " + foundUser.getAge() + " " + foundUser.getDescription());
        }
    }

    private void sendMessage(int userId, String message) {
        try {
            apiClient.messages()
                    .send(actor)
                    .message(message)
                    .userId(userId).randomId(random.nextInt()).execute();
            log.debug("message sent to userId={}", userId);
        } catch (ApiException e) {
            log.error("INVALID REQUEST", e);
        } catch (ClientException e) {
            log.error("NETWORK ERROR", e);
        }
    }
}
