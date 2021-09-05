package com.drychan.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class Keyboard {
    @JsonProperty("one_time")
    private boolean oneTime;
    private boolean inline;
    private Button[][] buttons;

    public static Keyboard likeNoKeyboard(boolean inline) {
        Button likeButton = new Button("positive", ButtonAction.builder()
                .type("text")
                .label("like")
                .build());
        Button noButton = new Button("negative", ButtonAction.builder()
                .type("text")
                .label("no")
                .build());
        Button[][] buttons = {{likeButton, noButton}};
        return new Keyboard(false, inline, buttons);
    }

    public static Keyboard helpKeyboard(boolean inline) {
        Button helpButton = new Button("secondary", ButtonAction.builder()
                .type("text")
                .label("help")
                .build());
        Button deleteButton = new Button("negative", ButtonAction.builder()
                .type("text")
                .label("delete")
                .build());
        Button[][] buttons = {{helpButton, deleteButton}};
        return new Keyboard(false, inline, buttons);
    }
}
