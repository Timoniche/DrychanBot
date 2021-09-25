package com.drychan.transformer;

import com.drychan.model.audio.MessageAudioAttachment;
import com.vk.api.sdk.objects.messages.AudioMessage;
import org.springframework.stereotype.Component;

@Component
public class AudioMessageTransformer {

    public MessageAudioAttachment transform(AudioMessage audioMessage) {
        return MessageAudioAttachment.builder()
                .id(audioMessage.getId())
                .ownerId(audioMessage.getOwnerId())
                .accessKey(audioMessage.getAccessKey())
                .linkMp3(audioMessage.getLinkMp3())
                .build();
    }
}
