package com.drychan.client;

import com.drychan.model.Keyboard;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vk.api.sdk.actions.Photos;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.actions.Messages;
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

    public static MessagesSendQuery addKeyboard(MessagesSendQuery query, Keyboard keyboard)
            throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String keyboardJson = objectMapper.writeValueAsString(keyboard);
        return query.unsafeParam(KEYBOARD_PARAM, keyboardJson);
    }
}
