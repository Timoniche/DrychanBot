package com.drychan.handler;

import com.drychan.model.Like;
import com.drychan.model.User;
import com.drychan.service.LikeService;
import com.drychan.service.UserService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.photos.Photo;
import com.vk.api.sdk.objects.photos.PhotoUpload;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import javax.imageio.ImageIO;

@Component
@Log4j2
public class MessageHandler {
    private final GroupActor actor;

    private final VkApiClient apiClient;

    private final Random random;

    private final UserService userService;

    private final LikeService likeService;

    private final int SEEN_CACHE_SIZE = 10000;

    //todo: setup redis
    private final LinkedHashMap<Integer, Integer> lastSeenProfile;

    private static final String NEXT_LINE = System.lineSeparator();

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

    public void handleMessage(int userId, String message, PhotoAttachment photoAttachment) {
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
                processDraftUser(user, userId, message, photoAttachment);
            } else {
                processPublishedUser(user, userId, message);
            }
        }
    }

    //todo: refactor to another class
    public Optional<PhotoAttachment> resolvePhotoAttachment(JsonArray attachments) {
        for (JsonElement attachmentElement : attachments) {
            JsonObject attachment = attachmentElement.getAsJsonObject();
            String attachmentType = attachment.get("type").getAsString();
            if (attachmentType.equals("photo")) {
                JsonObject photoObject = attachment.get("photo").getAsJsonObject();
                int photoId = photoObject.get("id").getAsInt();
                int photoOwnerId = photoObject.get("owner_id").getAsInt();
                String accessKey = null;
                JsonElement accessKeyElement = photoObject.get("access_key");
                if (accessKeyElement != null) {
                    //vk doesn't allow resent photos protected by an access_key
                    //so we have to download photo and upload it from us
                    try {
                        PhotoUpload photoUpload = apiClient.photos()
                                .getMessagesUploadServer(actor)
                                .execute();
                        String uploadUrl = photoUpload.getUploadUrl();
                        HttpEntity responseEntity = uploadPhotoByUrl(uploadUrl,
                                streamFromPhotoUrl(getBestLinkToLoadFrom(photoObject)));
                        if (responseEntity == null) {
                            log.warn("Photo with url {} not uploaded", uploadUrl);
                            return Optional.empty();
                        }
                        String uploadedPhotoJSON = EntityUtils.toString(responseEntity);
                        log.info("uploaded photoJSON: {}", uploadedPhotoJSON);
                        JsonParser parser = new JsonParser();
                        JsonElement jsonElement = parser.parse(uploadedPhotoJSON);
                        JsonObject rootJsonObject = jsonElement.getAsJsonObject();
                        String photo = rootJsonObject.get("photo").getAsString();
                        Integer server = rootJsonObject.get("server").getAsInt();
                        String hash = rootJsonObject.get("hash").getAsString();
                        List<Photo> uploadedPhotos = apiClient.photos()
                                .saveMessagesPhoto(actor, photo)
                                .server(server)
                                .hash(hash)
                                .execute();
                        if (uploadedPhotos.isEmpty()) {
                            return Optional.empty();
                        }
                        Photo uploadedPhoto = uploadedPhotos.get(0);
                        photoOwnerId = uploadedPhoto.getOwnerId();
                        photoId = uploadedPhoto.getId();
                        accessKey = uploadedPhoto.getAccessKey();
                    } catch (ClientException | ApiException | IOException ex) {
                        return Optional.empty();
                    }
                }
                return Optional.of(new PhotoAttachment(photoOwnerId, photoId, accessKey));
            }
        }
        return Optional.empty();
    }

    private String getBestLinkToLoadFrom(JsonObject photoObject) {
        String bestQualityPhotoUrl = "";
        if (photoObject.get("photo_2560") != null) {
            bestQualityPhotoUrl = photoObject.get("photo_2560").getAsString();
        } else if (photoObject.get("photo_1280") != null) {
            bestQualityPhotoUrl = photoObject.get("photo_1280").getAsString();
        } else if (photoObject.get("photo_604") != null) {
            bestQualityPhotoUrl = photoObject.get("photo_604").getAsString();
        }
        return bestQualityPhotoUrl;
    }

    private ByteArrayOutputStream streamFromPhotoUrl(String photoUrl) throws IOException {
        BufferedImage img = ImageIO.read(new URL(photoUrl));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", os);
        return os;
    }

    private HttpEntity uploadPhotoByUrl(String uploadUrl, ByteArrayOutputStream photoStream) {
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost uploadFile = new HttpPost(uploadUrl);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("field1", "yes", ContentType.TEXT_PLAIN);

            //todo: rewrite with streams
            File tmpFile = File.createTempFile("avatar", ".jpg");
            try (FileOutputStream out = new FileOutputStream(tmpFile)) {
                IOUtils.copy(new ByteArrayInputStream(photoStream.toByteArray()), out);
            }

            builder.addBinaryBody(
                    "file",
                    new FileInputStream(tmpFile),
                    ContentType.APPLICATION_OCTET_STREAM,
                    tmpFile.getName()
            );

            HttpEntity multipart = builder.build();
            uploadFile.setEntity(multipart);
            CloseableHttpResponse response = httpClient.execute(uploadFile);
            return response.getEntity();
        } catch (IOException ex) {
            log.warn("No response from url {}", uploadUrl);
            return null;
        }
    }

    //todo: refactor to separate functions
    private void processDraftUser(User user, int userId, String message, PhotoAttachment photoAttachment) {
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
                sendMessage(userId, "Теперь нужна красивая фото4ка!");
            }
        } else if (user.getPhotoPath() == null) {
            if (photoAttachment == null) {
                sendMessage(userId, "Не вижу твоей фотки, try one more time");
                return;
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
            sendMessage(userId, genderDependentQuestion);
        } else if (user.getAge() == null) {
            try {
                int age = Integer.parseInt(message);
                user.setAge(age);
                userService.saveUser(user);
                log.info("user_id={} set age to {}", userId, age);
                sendMessage(userId, "Придумаешь остроумное описание?");
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
                sendMessage(userId, "Ваша анкета: " +
                                NEXT_LINE + user.getName() +
                                NEXT_LINE + user.getAge() +
                                NEXT_LINE + user.getDescription(),
                        user.getPhotoPath());
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
                    Optional<User> lastSeenUser = userService.findById(lastSeenId);
                    if (lastSeenUser.isEmpty()) {
                        sendMessage(userId, "Пара вас лайкнула, но уже удалилась из приложения");
                        return;
                    }
                    sendMessage(userId, "match with ".concat(String.valueOf(lastSeenId)),
                            lastSeenUser.get().getPhotoPath());
                    sendMessage(lastSeenId, "match with ".concat(String.valueOf(userId)), user.getPhotoPath());
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
                    " [like/no]", foundUser.getPhotoPath());
            lastSeenProfile.put(userId, foundUser.getUserId());
        }
    }

    private void sendMessage(int userId, String message) {
        sendMessage(userId, message, null);
    }

    private void sendMessage(int userId, String message, String photoAttachmentPath) {
        try {
            if (userId < 1000) {
                log.info("message={} to fake account id={}", message, userId);
                return;
            }
            var sendQuery = apiClient.messages()
                    .send(actor)
                    .message(message)
                    .userId(userId).randomId(random.nextInt());
            if (photoAttachmentPath != null) {
                sendQuery.attachment(photoAttachmentPath);
            }
            sendQuery.execute();
            log.info("message={} sent to userId={}", message, userId);
        } catch (ApiException e) {
            log.error("INVALID REQUEST", e);
        } catch (ClientException e) {
            log.error("NETWORK ERROR", e);
        }
    }

    @RequiredArgsConstructor
    public static class PhotoAttachment {
        private final int ownerId;
        private final int id;
        private final String accessKey;

        /**
         * for group owner insert '-' before ownerId
         */
        @Override
        public String toString() {
            String stringView = "photo" + ownerId + "_" + id;
            if (accessKey != null) {
                stringView += "_" + accessKey;
            }
            return stringView;
        }
    }
}
