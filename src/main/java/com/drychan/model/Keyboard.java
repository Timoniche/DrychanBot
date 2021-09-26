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
import static com.drychan.model.ButtonColor.PRIMARY;
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
    public static final String FEMALE = "\uD83D\uDE4E\u200D♀";
    public static final String NOT_AGAIN = "Только не это...";
    public static final String START = "start";
    public static final String YEEES = "Да!";
    public static final String APPROVE = "Погнали!✅";

    public static final Button helpButton = new Button(SECONDARY.getColor(), ButtonAction.builder()
            .type(TEXT_BUTTON_TYPE)
            .label(HELP.getCommand())
            .build());

    public static final Button deleteButton = new Button(NEGATIVE.getColor(), ButtonAction.builder()
            .type(TEXT_BUTTON_TYPE)
            .label(DELETE.getCommand())
            .build());

    public static final Button maleButton = new Button(SECONDARY.getColor(), ButtonAction.builder()
            .type(TEXT_BUTTON_TYPE)
            .label(MALE)
            .build());

    public static final Button malePrimaryButton = new Button(PRIMARY.getColor(), ButtonAction.builder()
            .type(TEXT_BUTTON_TYPE)
            .label(MALE)
            .build());

    public static final Button femaleButton = new Button(SECONDARY.getColor(), ButtonAction.builder()
            .type(TEXT_BUTTON_TYPE)
            .label(FEMALE)
            .build());

    public static final Button femalePrimaryButton = new Button(PRIMARY.getColor(), ButtonAction.builder()
            .type(TEXT_BUTTON_TYPE)
            .label(FEMALE)
            .build());

    public static final Button likeButton = new Button(POSITIVE.getColor(), ButtonAction.builder()
            .type(TEXT_BUTTON_TYPE)
            .label(LIKE)
            .build());

    public static final Button noButton = new Button(NEGATIVE.getColor(), ButtonAction.builder()
            .type(TEXT_BUTTON_TYPE)
            .label(DISLIKE)
            .build());

    public static final Button yesButton = new Button(POSITIVE.getColor(), ButtonAction.builder()
            .type(TEXT_BUTTON_TYPE)
            .label(YEEES)
            .build());

    public static final Button notAgainButton = new Button(PRIMARY.getColor(), ButtonAction.builder()
            .type(TEXT_BUTTON_TYPE)
            .label(NOT_AGAIN)
            .build());

    public static final Button startButton = new Button(PRIMARY.getColor(), ButtonAction.builder()
            .type(TEXT_BUTTON_TYPE)
            .label(START)
            .build());

    public static final Button approveButton = new Button(POSITIVE.getColor(), ButtonAction.builder()
            .type(TEXT_BUTTON_TYPE)
            .label(APPROVE)
            .build());

    public static Keyboard keyboardFromButton(Button button, boolean inline) {
        Button[][] buttons = {{button}};
        return new Keyboard(false, inline, buttons);
    }

    public static Keyboard keyboardFromButtons(Button button1, Button button2, boolean inline) {
        Button[][] buttons = {{button1, button2}};
        return new Keyboard(false, inline, buttons);
    }

    public static Keyboard genderKeyboard(boolean inline) {
        Button[][] buttons = {{maleButton, femaleButton}};
        return new Keyboard(false, inline, buttons);
    }

    public static Keyboard likeNoKeyboard(boolean inline) {
        Button[][] buttons = {{likeButton, noButton}};
        return new Keyboard(false, inline, buttons);
    }

    public static Keyboard yesOrNotAgainKeyboard(boolean inline) {
        Button[][] buttons = {{yesButton, notAgainButton}};
        return new Keyboard(false, inline, buttons);
    }

    public static Keyboard startKeyboard(boolean inline) {
        Button[][] buttons = {{startButton}};
        return new Keyboard(false, inline, buttons);
    }

    public static Keyboard approveKeyboard(boolean inline) {
        Button[][] buttons = {{approveButton}};
        return new Keyboard(false, inline, buttons);
    }

    public static Keyboard approveHelpKeyboard(boolean inline) {
        Button[][] buttons = {{approveButton, helpButton}};
        return new Keyboard(false, inline, buttons);
    }

    public static Keyboard helpKeyboard(boolean inline) {
        Button[][] buttons = {{helpButton, deleteButton}};
        return new Keyboard(false, inline, buttons);
    }
}
