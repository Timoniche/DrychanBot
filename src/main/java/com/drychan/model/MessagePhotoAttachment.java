package com.drychan.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class MessagePhotoAttachment {
    @JsonProperty("album_id")
    private long albumId;
    private long date;
    private long id;
    @JsonProperty("owner_id")
    private long ownerId;
    @JsonProperty("access_key")
    private String accessKey;
    private String photo_2560;
    private String photo_1280;
    private String photo_807;
    private String photo_604;
    private String photo_130;
    private String photo_75;
    private int height;
    private int width;

    @Override
    public String toString() {
        String stringView = "photo" + ownerId + "_" + id;
        if (accessKey != null) {
            stringView += "_" + accessKey;
        }
        return stringView;
    }
}
