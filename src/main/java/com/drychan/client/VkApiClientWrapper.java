package com.drychan.client;

import com.drychan.model.Keyboard;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vk.api.sdk.actions.Photos;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.actions.Messages;
import com.vk.api.sdk.queries.docs.DocsGetMessagesUploadServerQuery;
import com.vk.api.sdk.queries.docs.DocsSaveQuery;
import com.vk.api.sdk.queries.messages.MessagesSendQuery;

/**
 * VkApiClient doesn't support new features like keyboard
 */
public class VkApiClientWrapper {
    private static final String KEYBOARD_PARAM = "keyboard";

    private final VkApiClient vkApiClient;

    public VkApiClientWrapper() {
        HttpTransportClient client = new HttpTransportClient();
        vkApiClient = new VkApiClient(client);
    }

    public Messages messages() {
        return vkApiClient.messages();
    }

    public Photos photos() {
        return vkApiClient.photos();
    }

    public DocsGetMessagesUploadServerQuery docsMessagesUploadServer(GroupActor groupActor) {
        return vkApiClient.docs().getMessagesUploadServer(groupActor);
    }

    public DocsSaveQuery saveDocFile(GroupActor groupActor, String file) {
        return vkApiClient.docs().save(groupActor, file);
    }

    public static MessagesSendQuery addKeyboard(MessagesSendQuery query, Keyboard keyboard)
            throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String keyboardJson = objectMapper.writeValueAsString(keyboard);
        return query.unsafeParam(KEYBOARD_PARAM, keyboardJson);
    }
}
