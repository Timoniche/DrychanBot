package com.drychan.controller;

import java.util.Optional;

import com.drychan.handler.MessageHandler;
import com.drychan.model.PhotoAttachment;
import com.drychan.model.UserMessage;
import com.google.gson.JsonArray;
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

    private static final String CONFIRMATION_TYPE = "confirmation";
    private static final String MESSAGE_TYPE = "message_new";
    private static final String OK_BODY = "ok";

    public VkController(@Value("${confirmation.code}") String confirmationCode,
                        MessageHandler messageHandler) {
        this.confirmationCode = confirmationCode;
        this.messageHandler = messageHandler;
    }

    @PostMapping("/")
    public String doChatBotResponse(@RequestBody String incomingJson) {
        log.info(incomingJson);
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
                JsonElement maybeAttachments = childJsonObject.get("attachments");
                Optional<PhotoAttachment> maybePhoto = Optional.empty();
                if (maybeAttachments != null) {
                    JsonArray attachments = childJsonObject.get("attachments").getAsJsonArray();
                    maybePhoto = messageHandler.resolvePhotoAttachment(attachments);
                }
                messageHandler.handleMessage(userId, new UserMessage(message, maybePhoto.orElse(null)));
                responseBody = OK_BODY;
                break;
            default:
                responseBody = OK_BODY;
                break;
        }

        return responseBody;
    }
}
