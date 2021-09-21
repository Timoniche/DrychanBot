package com.drychan.model;

import java.net.URI;
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

    public URI getBestLinkToLoadFrom() {
        PhotoSizeDescendingComparator photosQualityComparator = new PhotoSizeDescendingComparator();
        Arrays.sort(sizes, photosQualityComparator);
        return sizes[0].getUri();
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
