package com.drychan.model;

public enum ButtonColor {
    PRIMARY("primary"),
    SECONDARY("secondary"),
    NEGATIVE("negative"),
    POSITIVE("positive");

    private final String color;

    ButtonColor(String color) {
        this.color = color;
    }

    public String getColor() {
        return color;
    }
}
