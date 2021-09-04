package com.drychan.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Data;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Data
public class UploadedPhotoTo {
    private String photo;
    private int server;
    private String hash;
}
