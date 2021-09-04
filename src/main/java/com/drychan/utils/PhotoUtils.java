package com.drychan.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;

import com.drychan.dto.UploadedPhotoTo;
import com.drychan.model.MessagePhotoAttachment;
import com.drychan.transformer.PhotoTransformer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.photos.Photo;
import com.vk.api.sdk.objects.photos.PhotoUpload;
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

@Log4j2
public class PhotoUtils {

    private final GroupActor actor;

    private final VkApiClient apiClient;

    private final PhotoTransformer photoTransformer;

    public PhotoUtils(GroupActor actor, VkApiClient apiClient, PhotoTransformer photoTransformer) {
        this.actor = actor;
        this.apiClient = apiClient;
        this.photoTransformer = photoTransformer;
    }

    public static InputStream streamFromBestPhotoUrl(String bestPhotoUrl) throws IOException {
        BufferedImage img = ImageIO.read(new URL(bestPhotoUrl));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", os);
        return new ByteArrayInputStream(os.toByteArray());
    }

    public static HttpEntity uploadPhotoByUrl(String uploadUrl, InputStream photoStream) {
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost uploadFile = new HttpPost(uploadUrl);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("field1", "yes", ContentType.TEXT_PLAIN);

            //todo: rewrite with streams
            File tmpFile = File.createTempFile("avatar", ".jpg");
            try (FileOutputStream out = new FileOutputStream(tmpFile)) {
                IOUtils.copy(photoStream, out);
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

            if (!tmpFile.delete()) {
                log.warn("can't delete tmpfile {}", tmpFile.toString());
            }
            return response.getEntity();
        } catch (IOException ex) {
            log.warn("No response from url {}", uploadUrl);
            return null;
        }
    }

    /**
     * Vk doesn't allow resent photos protected by an access_key
     * so sometimes we have to download photo and upload it from our side
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
}
