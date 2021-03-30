package com.drychan.controller;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.google.gson.JsonParser;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@RestController
public class VkController {

    private final Random random = new Random();

    private final String confirmationCode;

    private final GroupActor actor;

    private final VkApiClient apiClient;

    private final static String CONFIRMATION_TYPE = "confirmation";
    private final static String MESSAGE_TYPE = "message_new";
    private final static String OK_BODY = "ok";

    public VkController(@Value("${vk.token}") String token,
                        @Value("${group.id}") String groupIdAsString,
                        @Value("${server.id}") String serverIdAsString,
                        @Value("${confirmation.code}") String confirmationCode) {
        HttpTransportClient client = new HttpTransportClient();
        apiClient = new VkApiClient(client);
        actor = new GroupActor(Integer.parseInt(groupIdAsString), token);
        this.confirmationCode = confirmationCode;
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
                sendMessage(userId, "Привет, мир!");
                responseBody = OK_BODY;
                break;
            default:
                responseBody = OK_BODY;
                break;
        }

        return responseBody;
    }

    private void sendMessage(int userId, String message) {
        try {
            apiClient.messages()
                    .send(actor)
                    .message(message)
                    .userId(userId).randomId(random.nextInt()).execute();
        } catch (ApiException e) {
            // LOG.error("INVALID REQUEST", e);
        } catch (ClientException e) {
            // LOG.error("NETWORK ERROR", e);
        }
    }

}
