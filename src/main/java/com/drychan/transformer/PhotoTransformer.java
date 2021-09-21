package com.drychan.transformer;

import java.util.List;

import com.drychan.model.MessagePhotoAttachment;
import com.drychan.model.PhotoAttachmentSize;
import com.vk.api.sdk.objects.photos.PhotoSizes;
import com.vk.api.sdk.objects.photos.responses.SaveMessagesPhotoResponse;
import org.springframework.stereotype.Component;

import static com.drychan.model.PhotoAttachmentSizeType.typeFromVkPhotoType;

@Component
public class PhotoTransformer {

    public MessagePhotoAttachment transform(SaveMessagesPhotoResponse photo) {
        return MessagePhotoAttachment.builder()
                .albumId(photo.getAlbumId())
                .date(photo.getDate())
                .id(photo.getId())
                .ownerId(photo.getOwnerId())
                .accessKey(photo.getAccessKey())
                .sizes(transform(photo.getSizes()))
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
                .uri(photoSizes.getSrc())
                .type(typeFromVkPhotoType(photoSizes.getType()))
                .build();
    }
}
