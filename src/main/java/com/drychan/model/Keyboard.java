package com.drychan.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.drychan.handler.DefaultCommands.DELETE;
import static com.drychan.handler.DefaultCommands.HELP;
import static com.drychan.model.ButtonColor.NEGATIVE;
import static com.drychan.model.ButtonColor.POSITIVE;
import static com.drychan.model.ButtonColor.SECONDARY;

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

    public static final String TEXT_BUTTON_TYPE = "text";
    public static final String LIKE = "\uD83D\uDC4D";
    public static final String DISLIKE = "\uD83D\uDC4E";
    public static final String MALE = "\uD83E\uDDD4";
    public static final String FEMALE = "\uD83D\uDC69\u200D\uD83E\uDDB0";

    public static Keyboard genderKeyboard(boolean inline) {
        Button maleButton = new Button(SECONDARY.getColor(), ButtonAction.builder()
                .type(TEXT_BUTTON_TYPE)
                .label(MALE)
                .build());
        Button femaleButton = new Button(SECONDARY.getColor(), ButtonAction.builder()
                .type(TEXT_BUTTON_TYPE)
                .label(FEMALE)
                .build());
        Button[][] buttons = {{maleButton, femaleButton}};
        return new Keyboard(false, inline, buttons);
    }

    public static Keyboard likeNoKeyboard(boolean inline) {
        Button likeButton = new Button(POSITIVE.getColor(), ButtonAction.builder()
                .type(TEXT_BUTTON_TYPE)
                .label(LIKE)
                .build());
        Button noButton = new Button(NEGATIVE.getColor(), ButtonAction.builder()
                .type(TEXT_BUTTON_TYPE)
                .label(DISLIKE)
                .build());
        Button[][] buttons = {{likeButton, noButton}};
        return new Keyboard(false, inline, buttons);
    }
}
