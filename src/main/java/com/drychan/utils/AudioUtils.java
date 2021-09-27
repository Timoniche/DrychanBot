package com.drychan.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import com.drychan.client.VkApiClientWrapper;
import com.drychan.model.DocFile;
import com.drychan.model.voice.MessageVoiceAttachment;
import com.drychan.transformer.AudioMessageTransformer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.base.responses.GetUploadServerResponse;
import com.vk.api.sdk.objects.docs.responses.SaveResponse;
import com.vk.api.sdk.objects.messages.AudioMessage;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

import static com.drychan.utils.UploadUtils.uploadFileByUrl;
import static com.vk.api.sdk.objects.docs.GetMessagesUploadServerType.AUDIO_MESSAGE;

@Log4j2
public class AudioUtils {

    private final GroupActor actor;

    private final VkApiClientWrapper apiClient;

    private final AudioMessageTransformer audioMessageTransformer;

    public AudioUtils(GroupActor actor, VkApiClientWrapper apiClient, AudioMessageTransformer audioMessageTransformer) {
        this.actor = actor;
        this.apiClient = apiClient;
        this.audioMessageTransformer = audioMessageTransformer;
    }

    public static void streamFromMp3Url(File fileToStore, URL mp3Url) throws IOException {
        URLConnection conn = mp3Url.openConnection();
        InputStream is = conn.getInputStream();

        OutputStream outstream = new FileOutputStream(fileToStore);
        byte[] buffer = new byte[4096];
        int len;
        while ((len = is.read(buffer)) > 0) {
            outstream.write(buffer, 0, len);
        }
        outstream.close();
    }

    public MessageVoiceAttachment reuploadAudio(MessageVoiceAttachment messageVoiceAttachment) {
        URI linkMp3ToLoadFrom = messageVoiceAttachment.getLinkMp3();
        if (linkMp3ToLoadFrom == null) {
            log.warn("No mp3 link to load from");
            return null;
        }
        try {
            GetUploadServerResponse audioMsgUpload = apiClient.docsMessagesUploadServer(actor)
                    .type(AUDIO_MESSAGE)
                    .peerId(messageVoiceAttachment.getOwnerId())
                    .execute();
            URI uploadUrl = audioMsgUpload.getUploadUrl();

            File tmpFile = File.createTempFile("audio", ".mp3");
            streamFromMp3Url(tmpFile, messageVoiceAttachment.getLinkMp3().toURL());
            HttpEntity responseEntity = uploadFileByUrl(uploadUrl, tmpFile);
            if (!tmpFile.delete()) {
                log.warn("can't delete tmpfile {}", tmpFile.toString());
            }
            if (responseEntity == null) {
                log.warn("Audio msg with url {} not reuploaded", messageVoiceAttachment.getLinkMp3());
                return null;
            }
            String uploadedAudioMsgJson = EntityUtils.toString(responseEntity);
            log.info("uploaded audio msgJson: {}", uploadedAudioMsgJson);
            ObjectMapper objectMapper = new ObjectMapper();
            DocFile uploadedVoiceFile = objectMapper.readValue(uploadedAudioMsgJson, DocFile.class);
            SaveResponse savedVoice = apiClient.saveDocFile(actor, uploadedVoiceFile.getFile()).execute();
            AudioMessage reuploadedAudioMsg = savedVoice.getAudioMessage();
            return audioMessageTransformer.transform(reuploadedAudioMsg);
        } catch (ApiException | ClientException | IOException e) {
            log.warn("Audio message {} not reuploaded", messageVoiceAttachment.toString());
            return null;
        }
    }

}
