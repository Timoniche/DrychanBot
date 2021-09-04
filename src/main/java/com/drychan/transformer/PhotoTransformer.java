package com.drychan.transformer;

import java.util.List;

import com.drychan.model.MessagePhotoAttachment;
import com.drychan.model.PhotoAttachmentSize;
import com.vk.api.sdk.objects.photos.Photo;
import com.vk.api.sdk.objects.photos.PhotoSizes;
import org.springframework.stereotype.Component;

import static com.drychan.model.PhotoAttachmentSizeType.typeFromVkPhotoType;

@Component
public class PhotoTransformer {

    public MessagePhotoAttachment transform(Photo photo) {
        return MessagePhotoAttachment.builder()
                .albumId(photo.getAlbumId())
                .date(photo.getDate())
                .id(photo.getId())
                .ownerId(photo.getOwnerId())
                .accessKey(photo.getAccessKey())
                .sizes(transform(photo.getSizes()))
                .photo_2560(photo.getPhoto2560())
                .photo_1280(photo.getPhoto1280())
                .photo_807(photo.getPhoto807())
                .photo_604(photo.getPhoto604())
                .photo_130(photo.getPhoto130())
                .photo_75(photo.getPhoto75())
                .build();
    }

    public PhotoAttachmentSize[] transform(List<PhotoSizes> photoSizess) {
        if (photoSizess == null) {
            return null;
        }
        return photoSizess.stream()
                .map(this::transform)
                .toArray(PhotoAttachmentSize[]::new);
    }

    public PhotoAttachmentSize transform(PhotoSizes photoSizes) {
        return PhotoAttachmentSize.builder()
                .height(photoSizes.getHeight())
                .width(photoSizes.getWidth())
                .url(photoSizes.getSrc())
                .type(typeFromVkPhotoType(photoSizes.getType()))
                .build();
    }
}
