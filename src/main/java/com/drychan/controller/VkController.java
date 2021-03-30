package com.drychan.controller;

import com.drychan.handler.MessageHandler;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.google.gson.JsonParser;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class VkController {
    private final String confirmationCode;

    private final MessageHandler messageHandler;

    private final static String CONFIRMATION_TYPE = "confirmation";
    private final static String MESSAGE_TYPE = "message_new";
    private final static String OK_BODY = "ok";

    public VkController(@Value("${vk.token}") String token,
                        @Value("${group.id}") String groupIdAsString,
                        @Value("${confirmation.code}") String confirmationCode) {
        HttpTransportClient client = new HttpTransportClient();
        VkApiClient apiClient = new VkApiClient(client);
        GroupActor actor = new GroupActor(Integer.parseInt(groupIdAsString), token);
        this.confirmationCode = confirmationCode;
        messageHandler = new MessageHandler(actor, apiClient);
    }

    @PostMapping("/")
    public String doChatBotResponse(@RequestBody String incomingJson) {
        JsonParser parser = new JsonParser();
        JsonElement jsonElement = parser.parse(incomingJson);
        JsonObject rootJsonObject = jsonElement.getAsJsonObject();
        String type = rootJsonObject.get("type").getAsString();

        final String responseBody;
        switch (type) {
            case CONFIRMATION_TYPE:
                responseBody = confirmationCode;
                break;
            case MESSAGE_TYPE:
                JsonObject childJsonObject = rootJsonObject.getAsJsonObject("object");
                String message = childJsonObject.get("body").getAsString();
                int userId = childJsonObject.get("user_id").getAsInt();
                messageHandler.handleMessage(userId, message);
                responseBody = OK_BODY;
                break;
            default:
                responseBody = OK_BODY;
                break;
        }

        return responseBody;
    }

}
