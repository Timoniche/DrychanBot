package com.drychan.model;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ObjectMessage {
    private long id;
    private long date;
    @JsonProperty("from_id")
    private int userId;
    private String text;
    private MessageNewAttachments[] attachments;

    private static final String PHOTO_TYPE = "photo";

    public Optional<MessagePhotoAttachment> findAnyPhotoAttachment() {
        if (attachments == null || attachments.length == 0) {
            return Optional.empty();
        }
        for (MessageNewAttachments attachment : attachments) {
            if (attachment.getType().equals(PHOTO_TYPE)) {
                return Optional.of(attachment.getPhoto());
            }
        }
        return Optional.empty();
    }
}
