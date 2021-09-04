package com.drychan.model;

import java.util.Arrays;

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
public class MessagePhotoAttachment {
    @JsonProperty("album_id")
    private long albumId;
    private long date;
    private long id;
    @JsonProperty("owner_id")
    private long ownerId;
    @JsonProperty("access_key")
    private String accessKey;
    private PhotoAttachmentSize[] sizes;
    private String photo_2560;
    private String photo_1280;
    private String photo_807;
    private String photo_604;
    private String photo_130;
    private String photo_75;

    public String getBestLinkToLoadFrom() {
        if (sizes == null || sizes.length == 0) {
            return getBestLinkToLoadFromV5_50();
        } else {
            PhotoSizeDescendingComparator photosQualityComparator = new PhotoSizeDescendingComparator();
            Arrays.sort(sizes, photosQualityComparator);
            return sizes[0].getUrl();
        }
    }

    public String getBestLinkToLoadFromV5_50() {
        if (photo_2560 != null) {
            return photo_2560;
        } else if (photo_1280 != null) {
            return photo_1280;
        } else if (photo_807 != null) {
            return photo_807;
        } else if (photo_604 != null) {
            return photo_604;
        } else if (photo_130 != null) {
            return photo_130;
        } else if (photo_75 != null) {
            return photo_75;
        }
        return null;
    }

    @Override
    public String toString() {
        String stringView = "photo" + ownerId + "_" + id;
        if (accessKey != null) {
            stringView += "_" + accessKey;
        }
        return stringView;
    }
}
