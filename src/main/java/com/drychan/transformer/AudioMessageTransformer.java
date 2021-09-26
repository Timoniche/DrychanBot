package com.drychan.transformer;

import com.drychan.model.voice.MessageVoiceAttachment;
import com.vk.api.sdk.objects.messages.AudioMessage;
import org.springframework.stereotype.Component;

@Component
public class AudioMessageTransformer {

    public MessageVoiceAttachment transform(AudioMessage audioMessage) {
        return MessageVoiceAttachment.builder()
                .id(audioMessage.getId())
                .ownerId(audioMessage.getOwnerId())
                .accessKey(audioMessage.getAccessKey())
                .linkMp3(audioMessage.getLinkMp3())
                .build();
    }
}
