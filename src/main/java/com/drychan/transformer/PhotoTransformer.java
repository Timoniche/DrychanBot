package com.drychan.transformer;

import com.drychan.model.MessagePhotoAttachment;
import com.vk.api.sdk.objects.photos.Photo;
import org.springframework.stereotype.Component;

@Component
public class PhotoTransformer {

    public MessagePhotoAttachment transform(Photo photo) {
        return MessagePhotoAttachment.builder()
                .albumId(photo.getAlbumId())
                .date(photo.getDate())
                .id(photo.getId())
                .ownerId(photo.getOwnerId())
                .accessKey(photo.getAccessKey())
                .photo_2560(photo.getPhoto2560())
                .photo_1280(photo.getPhoto1280())
                .photo_807(photo.getPhoto807())
                .photo_604(photo.getPhoto604())
                .photo_130(photo.getPhoto130())
                .photo_75(photo.getPhoto75())
                .height(photo.getHeight())
                .width(photo.getWidth())
                .build();
    }
}
