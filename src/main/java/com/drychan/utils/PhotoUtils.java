package com.drychan.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;

import com.drychan.client.VkApiClientWrapper;
import com.drychan.dto.UploadedPhotoTo;
import com.drychan.model.MessagePhotoAttachment;
import com.drychan.transformer.PhotoTransformer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.photos.responses.GetMessagesUploadServerResponse;
import com.vk.api.sdk.objects.photos.responses.SaveMessagesPhotoResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

import static com.drychan.utils.UploadUtils.uploadFileByUrl;

@Log4j2
public class PhotoUtils {

    private final GroupActor actor;

    private final VkApiClientWrapper apiClient;

    private final PhotoTransformer photoTransformer;

    public PhotoUtils(GroupActor actor, VkApiClientWrapper apiClient, PhotoTransformer photoTransformer) {
        this.actor = actor;
        this.apiClient = apiClient;
        this.photoTransformer = photoTransformer;
    }

    public static InputStream streamFromBestPhotoUrl(URL bestPhotoUrl) throws IOException {
        BufferedImage img = ImageIO.read(bestPhotoUrl);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", os);
        return new ByteArrayInputStream(os.toByteArray());
    }

    @SuppressWarnings("unused")
    public HttpEntity uploadPhotoByUrl(URI uploadUrl, File photo) {
        try {
            return uploadFileByUrl(uploadUrl, photo);
        } catch (IOException ex) {
            log.warn("No response from url {}", uploadUrl);
            return null;
        }
    }

    public HttpEntity uploadPhotoByUrl(URI uploadUrl, InputStream photoStream) {
        try {
            //todo: rewrite with streams
            File tmpFile = File.createTempFile("avatar", ".jpg");
            try (FileOutputStream out = new FileOutputStream(tmpFile)) {
                IOUtils.copy(photoStream, out);
            }
            HttpEntity photoUploadResponse = uploadFileByUrl(uploadUrl, tmpFile);
            if (!tmpFile.delete()) {
                log.warn("can't delete tmpfile {}", tmpFile.toString());
            }
            return photoUploadResponse;
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
        URI bestLinkToLoadFrom = messagePhotoAttachment.getBestLinkToLoadFrom();
        return reuploadPhoto(bestLinkToLoadFrom);
    }

    public MessagePhotoAttachment reuploadPhoto(URI linkToLoadPhotoFrom) {
        try {
            if (linkToLoadPhotoFrom == null) {
                log.warn("No best link to load from");
                return null;
            }
            GetMessagesUploadServerResponse photoUpload = apiClient.photos()
                    .getMessagesUploadServer(actor)
                    .execute();
            URI uploadUrl = photoUpload.getUploadUrl();
            HttpEntity responseEntity = uploadPhotoByUrl(uploadUrl,
                    streamFromBestPhotoUrl(linkToLoadPhotoFrom.toURL()));
            if (responseEntity == null) {
                log.warn("Photo with url {} not uploaded", uploadUrl);
                return null;
            }
            return parseResponseEntity(responseEntity);
        } catch (ClientException | ApiException | IOException ex) {
            log.warn("Photo {} not reuploaded, ex: {}", linkToLoadPhotoFrom, ex.getMessage());
            return null;
        }
    }

    public MessagePhotoAttachment reuploadPhoto(InputStream photoInputStream) {
        try {
            GetMessagesUploadServerResponse photoUpload = apiClient.photos()
                    .getMessagesUploadServer(actor)
                    .execute();
            URI uploadUrl = photoUpload.getUploadUrl();
            HttpEntity responseEntity = uploadPhotoByUrl(uploadUrl, photoInputStream);
            if (responseEntity == null) {
                log.warn("Photo with url {} not uploaded", uploadUrl);
                return null;
            }
            return parseResponseEntity(responseEntity);
        } catch (ClientException | ApiException | IOException ex) {
            log.warn("Photo not reuploaded, ex: {}", ex.getMessage());
            return null;
        }
    }

    private MessagePhotoAttachment parseResponseEntity(HttpEntity responseEntity)
            throws IOException, ClientException, ApiException {
        String uploadedPhotoJson = EntityUtils.toString(responseEntity);
        log.info("uploaded photoJson: {}", uploadedPhotoJson);
        ObjectMapper objectMapper = new ObjectMapper();
        UploadedPhotoTo uploadedPhotoTo = objectMapper.readValue(uploadedPhotoJson, UploadedPhotoTo.class);
        List<SaveMessagesPhotoResponse> uploadedPhotos = apiClient.photos()
                .saveMessagesPhoto(actor, uploadedPhotoTo.getPhoto())
                .server(uploadedPhotoTo.getServer())
                .hash(uploadedPhotoTo.getHash())
                .execute();
        if (uploadedPhotos.isEmpty()) {
            return null;
        }
        SaveMessagesPhotoResponse uploadedPhoto = uploadedPhotos.get(0);
        return photoTransformer.transform(uploadedPhoto);
    }
}
