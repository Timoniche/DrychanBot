package com.drychan.handler;

import java.util.Random;

import com.drychan.client.VkApiClientWrapper;
import com.drychan.model.Keyboard;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class MessageSender {
    private final GroupActor actor;
    private final VkApiClientWrapper apiClient;
    private final Random random;

    public MessageSender(GroupActor actor, VkApiClientWrapper apiClient) {
        this.actor = actor;
        this.apiClient = apiClient;
        random = new Random();
    }

    public void send(int userId, String message) {
        send(userId, message, null, null);
    }

    public void send(int userId, String message, String photoAttachmentPath, Keyboard keyboard) {
        try {
            if (userId < 0) {
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
            if (keyboard != null) {
                try {
                    sendQuery = VkApiClientWrapper.addKeyboard(sendQuery, keyboard);
                } catch (JsonProcessingException ex) {
                    log.warn("Keyboard wasn't added to message to userId {}: {}", userId, ex.getMessage());
                }
            }
            sendQuery.execute();
            log.info("message={} sent to userId={}", message, userId);
        } catch (ApiException e) {
            log.error("INVALID REQUEST", e);
        } catch (ClientException e) {
            log.error("NETWORK ERROR", e);
        }
    }
}
