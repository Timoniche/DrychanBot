package com.drychan.handler;

import com.drychan.dao.model.Like;
import com.drychan.dao.model.User;
import com.drychan.dto.UploadedPhotoTo;
import com.drychan.model.MessageNewObject;
import com.drychan.model.MessagePhotoAttachment;
import com.drychan.service.LikeService;
import com.drychan.service.UserService;
import com.drychan.transformer.PhotoTransformer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.photos.Photo;
import com.vk.api.sdk.objects.photos.PhotoUpload;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.drychan.utils.PhotoUtils.streamFromBestPhotoUrl;
import static com.drychan.utils.PhotoUtils.uploadPhotoByUrl;

@Component
@Log4j2
public class MessageHandler {
    private final GroupActor actor;

    private final VkApiClient apiClient;

    private final MessageSender messageSender;

    private final UserService userService;

    private final LikeService likeService;

    private final PhotoTransformer photoTransformer;

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
        this.photoTransformer = photoTransformer;
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

    /**
     * Vk doesn't allow resent photos protected by an access_key
     * so we have to download photo and upload it from us
     */
    public MessagePhotoAttachment reuploadPhoto(MessagePhotoAttachment messagePhotoAttachment) {
        try {
            PhotoUpload photoUpload = apiClient.photos()
                    .getMessagesUploadServer(actor)
                    .execute();
            String uploadUrl = photoUpload.getUploadUrl();
            HttpEntity responseEntity = uploadPhotoByUrl(uploadUrl,
                    streamFromBestPhotoUrl(messagePhotoAttachment.getBestLinkToLoadFrom()));
            if (responseEntity == null) {
                log.warn("Photo with url {} not uploaded", uploadUrl);
                return null;
            }
            String uploadedPhotoJson = EntityUtils.toString(responseEntity);
            log.info("uploaded photoJson: {}", uploadedPhotoJson);
            ObjectMapper objectMapper = new ObjectMapper();
            UploadedPhotoTo uploadedPhotoTo = objectMapper.readValue(uploadedPhotoJson, UploadedPhotoTo.class);
            List<Photo> uploadedPhotos = apiClient.photos()
                    .saveMessagesPhoto(actor, uploadedPhotoTo.getPhoto())
                    .server(uploadedPhotoTo.getServer())
                    .hash(uploadedPhotoTo.getHash())
                    .execute();
            if (uploadedPhotos.isEmpty()) {
                return null;
            }
            Photo uploadedPhoto = uploadedPhotos.get(0);
            return photoTransformer.transform(uploadedPhoto);
        } catch (ClientException | ApiException | IOException ex) {
            return null;
        }
    }

    //todo: refactor to separate functions
    private void processDraftUser(User user, MessageNewObject message) {
        String messageText = message.getBody();
        int userId = user.getUserId();
        if (user.getName() == null) {
            if (messageText.isBlank()) {
                messageSender.send(userId, "Ты уверен, что твое имя на Whitespace?)");
            } else {
                user.setName(messageText);
                userService.saveUser(user);
                log.info("user_id={} set name to {}", userId, messageText);
                messageSender.send(userId, "Прекрасное имя! Теперь укажи свой пол) [м/ж]");
            }
        } else if (user.getGender() == null) {
            if (!messageText.equalsIgnoreCase("м") && !messageText.equalsIgnoreCase("ж")) {
                messageSender.send(userId, "Есть всего 2 гендера, м и ж, попробуй еще раз)");
            } else {
                boolean isMale = messageText.equalsIgnoreCase("м");
                if (isMale) {
                    user.setGender('m');
                } else {
                    user.setGender('f');
                }
                userService.saveUser(user);
                log.info("user_id={} set gender to {}", userId, messageText);
                messageSender.send(userId, "Теперь нужна красивая фото4ка!");
            }
        } else if (user.getPhotoPath() == null) {
            var maybePhotoAttachment = message.findAnyPhotoAttachment();
            MessagePhotoAttachment photoAttachment = maybePhotoAttachment.orElse(null);
            if (photoAttachment == null) {
                messageSender.send(userId, "Не вижу твоей фотки, try one more time");
                return;
            }
            if (photoAttachment.getAccessKey() != null) {
                photoAttachment = reuploadPhoto(photoAttachment);
                if (photoAttachment == null) {
                    messageSender.send(userId, "Не удалось загрузить фото, try one more time");
                    return;
                }
            }
            user.setPhotoPath(photoAttachment.toString());
            userService.saveUser(user);
            log.info("user_id={} set photo_path to {}", userId, photoAttachment.toString());

            String genderDependentQuestion;
            boolean isMale = user.getGender().equals('m');
            if (isMale) {
                genderDependentQuestion = "Сколько тебе лет, парень? Надеюсь, ты пришел не пикапить школьниц)";
            } else {
                genderDependentQuestion = "У девушки, конечно, невежливо спрашивать возраст, но я рискну)";
            }
            messageSender.send(userId, genderDependentQuestion);
        } else if (user.getAge() == null) {
            try {
                int age = Integer.parseInt(messageText);
                user.setAge(age);
                userService.saveUser(user);
                log.info("user_id={} set age to {}", userId, age);
                messageSender.send(userId, "Придумаешь остроумное описание?");
            } catch (NumberFormatException ex) {
                messageSender.send(userId, "Столько не живут)");
            }
        } else {
            if (messageText.isBlank()) {
                messageSender.send(userId, "Хм, немногословно) Попробуй еще раз!");
            } else {
                user.setDescription(messageText);
                user.setStatus(User.Status.published);
                userService.saveUser(user);
                log.info("user_id={} set description to {}", userId, messageText);
                log.info("user_id={} is published", userId);
                messageSender.send(userId, "Ваша анкета: " +
                                NEXT_LINE + user.getName() +
                                NEXT_LINE + user.getAge() +
                                NEXT_LINE + user.getDescription(),
                        user.getPhotoPath());
                suggestProfile(user.getGender(), userId);
            }
        }
    }

    private void processPublishedUser(User user, MessageNewObject message) {
        int userId = user.getUserId();
        String messageText = message.getBody();
        Integer lastSeenId = lastSeenProfile.get(userId);
        if (lastSeenId == null) {
            messageSender.send(userId, "Прошло слишком много времени");
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
            }
        }
        suggestProfile(user.getGender(), userId);
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
            messageSender.send(userId, foundUser.getName() + " " + foundUser.getAge() +
                    " " + foundUser.getDescription() +
                    " [like/no]", foundUser.getPhotoPath());
            lastSeenProfile.put(userId, foundUser.getUserId());
        }
    }

}
