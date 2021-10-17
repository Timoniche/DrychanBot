package com.drychan.controller;

import com.drychan.handler.MessageHandler;
import com.drychan.model.MessageNew;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final String TYPE_FIELD = "type";

    public VkController(@Value("${confirmation.code}") String confirmationCode,
                        MessageHandler messageHandler) {
        this.confirmationCode = confirmationCode;
        this.messageHandler = messageHandler;
    }

    @PostMapping
    public String vkHandler(@RequestBody String groupEventJson) {
        log.info("group event: {}", groupEventJson);
        switch (getEventType(groupEventJson)) {
            case CONFIRMATION_TYPE:
                return confirmationCode;
            case MESSAGE_TYPE:
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    MessageNew messageNew = objectMapper.readValue(groupEventJson, MessageNew.class);
                    messageHandler.handleMessage(messageNew.getObject().getMessage());
                } catch (JsonProcessingException ex) {
                    log.warn("groupEventJson not parsed: {}", ex.getMessage());
                }
                return OK_BODY;
            default:
                return OK_BODY;
        }
    }

    private String getEventType(String groupEventJson) {
        JsonParser parser = new JsonParser();
        JsonElement jsonElement = parser.parse(groupEventJson);
        JsonObject rootJsonObject = jsonElement.getAsJsonObject();
        return rootJsonObject.get(TYPE_FIELD).getAsString();
    }
}
