package com.drychan.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vk.api.sdk.objects.photos.PhotoSizesType;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
public enum PhotoAttachmentSizeType {
    @JsonProperty("s")
    S("s", 0),
    @JsonProperty("m")
    M("m", 1),
    @JsonProperty("x")
    X("x", 2),
    @JsonProperty("o")
    O("o", 3),
    @JsonProperty("p")
    P("p", 4),
    @JsonProperty("q")
    Q("q", 5),
    @JsonProperty("r")
    R("r", 6),
    @JsonProperty("y")
    Y("y", 7),
    @JsonProperty("z")
    Z("z", 8),
    @JsonProperty("w")
    W("w", 9);

    private final String value;
    private final int priority;

    PhotoAttachmentSizeType(String value, int priority) {
        this.value = value;
        this.priority = priority;
    }

    public String getValue() {
        return this.value;
    }

    public int getPriority() { return this.priority; }

    public static PhotoAttachmentSizeType typeFromVkPhotoType(PhotoSizesType type) {
        for (var ownType : PhotoAttachmentSizeType.values()) {
            if (ownType.value.equals(type.getValue())) {
                return ownType;
            }
        }
        return null;
    }
}