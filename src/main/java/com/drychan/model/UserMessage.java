package com.drychan.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class UserMessage {
    private final String message;
    private final PhotoAttachment photoAttachment;
}
