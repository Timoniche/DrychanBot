package com.drychan.model;

import java.net.URI;
import java.util.Comparator;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public class PhotoAttachmentSize {
    private int height;
    private URI url;
    private PhotoAttachmentSizeType type;
    private int width;
}

class PhotoSizeDescendingComparator implements Comparator<PhotoAttachmentSize>
{
    public int compare(PhotoAttachmentSize o1, PhotoAttachmentSize o2)
    {
        return o2.getType().getPriority() - o1.getType().getPriority();
    }
}
