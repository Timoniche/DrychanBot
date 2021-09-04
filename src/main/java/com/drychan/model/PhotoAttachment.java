package com.drychan.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PhotoAttachment {
    private final int ownerId;
    private final int id;
    private final String accessKey;

    @Override
    public String toString() {
        String stringView = "photo" + ownerId + "_" + id;
        if (accessKey != null) {
            stringView += "_" + accessKey;
        }
        return stringView;
    }
}