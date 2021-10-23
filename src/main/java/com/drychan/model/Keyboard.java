package com.drychan.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.drychan.handler.DefaultCommandsProcessor.DefaultCommands.DELETE;
import static com.drychan.handler.DefaultCommandsProcessor.DefaultCommands.HELP;
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
    public static final String LIKE_LABEL = "\uD83D\uDC4D";
    public static final String DISLIKE_LABEL = "\uD83D\uDC4E";
    public static final String MALE_LABEL = "\uD83E\uDDD4";
    public static final String FEMALE_LABEL = "\uD83D\uDE4E\u200D♀";
    public static final String NOT_AGAIN = "Только не это...";
    public static final String START_LABEL = "начать";
    public static final String YEEES = "Да!";
    public static final String APPROVE_LABEL = "Погнали!✅";
    public static final String MIX_DISLIKED_PROFILES_LABEL = "Пересмотреть отклоненных";
    public static final String CHECK_NEW_PROFILES_LABEL = "Проверить новые анкеты";

    public static final Button helpButton = buttonOf(SECONDARY, HELP.getCommand());

    public static final Button deleteButton = buttonOf(NEGATIVE, DELETE.getCommand());

    public static final Button maleButton = buttonOf(SECONDARY, MALE_LABEL);

    public static final Button malePrimaryButton = buttonOf(PRIMARY, MALE_LABEL);

    public static final Button femaleButton = buttonOf(SECONDARY, FEMALE_LABEL);

    public static final Button femalePrimaryButton = buttonOf(PRIMARY, FEMALE_LABEL);

    public static final Button likeButton = buttonOf(POSITIVE, LIKE_LABEL);

    public static final Button noButton = buttonOf(NEGATIVE, DISLIKE_LABEL);

    public static final Button yesButton = buttonOf(POSITIVE, YEEES);

    public static final Button notAgainButton = buttonOf(PRIMARY, NOT_AGAIN);

    public static final Button startButton = buttonOf(PRIMARY, START_LABEL);

    public static final Button approveButton = buttonOf(POSITIVE, APPROVE_LABEL);

    public static final Button mixDislikedProfilesButton = buttonOf(POSITIVE, MIX_DISLIKED_PROFILES_LABEL);

    public static final Button checkNewProfilesButton = buttonOf(PRIMARY, CHECK_NEW_PROFILES_LABEL);

    public static Button buttonOf(ButtonColor buttonColor, String label) {
        return new Button(buttonColor.getColor(), ButtonAction.builder()
                .type(TEXT_BUTTON_TYPE)
                .label(label)
                .build());
    }

    public static Keyboard keyboardFromButton(Button button, boolean inline) {
        Button[][] buttons = {{button}};
        return new Keyboard(false, inline, buttons);
    }

    public static Keyboard keyboardFromTwoVerticalButtons(boolean inline, Button button1, Button button2) {
        Button[][] buttons = {{button1}, {button2}};
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

    public static Keyboard approveHelpKeyboard(boolean inline) {
        Button[][] buttons = {{approveButton, helpButton}};
        return new Keyboard(false, inline, buttons);
    }

    public static Keyboard likeNoHelpKeyboard(boolean inline) {
        Button[][] buttons = {{likeButton, noButton}, {helpButton}};
        return new Keyboard(false, inline, buttons);
    }
}
