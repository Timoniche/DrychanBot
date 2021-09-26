package com.drychan.model;

import com.drychan.model.voice.MessageVoiceAttachment;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import static com.drychan.model.ObjectMessage.AUDIO_TYPE;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class MessageNewAttachments {
    private String type;
    private MessagePhotoAttachment photo;
    @JsonProperty(AUDIO_TYPE)
    private MessageVoiceAttachment audio;
}
