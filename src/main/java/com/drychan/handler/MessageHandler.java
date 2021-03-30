package com.drychan.handler;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import lombok.extern.slf4j.Slf4j;
import java.util.Random;

@Slf4j
public class MessageHandler {
    private final GroupActor actor;

    private final VkApiClient apiClient;

    private final Random random;

    public MessageHandler(GroupActor actor, VkApiClient apiClient) {
        this.actor = actor;
        this.apiClient = apiClient;
        random = new Random();
    }

    public void handleMessage(int userId, String message) {
        sendMessage(userId, message);
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
