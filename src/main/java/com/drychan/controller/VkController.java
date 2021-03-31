package com.drychan.controller;

import com.drychan.handler.MessageHandler;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.google.gson.JsonParser;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Log4j2
public class VkController {
    private final String confirmationCode;

    private final MessageHandler messageHandler;

    private final static String CONFIRMATION_TYPE = "confirmation";
    private final static String MESSAGE_TYPE = "message_new";
    private final static String OK_BODY = "ok";

    public VkController(@Value("${confirmation.code}") String confirmationCode,
                        MessageHandler messageHandler) {
        this.confirmationCode = confirmationCode;
        this.messageHandler = messageHandler;
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
