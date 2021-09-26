package com.drychan.model.voice;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageVoiceAttachment {
    private long id;
    @JsonProperty("owner_id")
    private int ownerId;
    @JsonProperty("access_key")
    private String accessKey;
    @JsonProperty("link_mp3")
    private URI linkMp3;

    public static final String DOC_TYPE = "doc";

    public String getAttachmentPath() {
        String stringView = DOC_TYPE + ownerId + "_" + id;
        if (accessKey != null) {
            stringView += "_" + accessKey;
        }
        return stringView;
    }

    @Override
    public String toString() {
        return getAttachmentPath();
    }
}
