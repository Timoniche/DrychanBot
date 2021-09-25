package com.drychan.handler;

import com.drychan.model.Button;
import com.drychan.model.ButtonAction;
import com.drychan.model.Keyboard;
import com.drychan.service.LikeService;
import com.drychan.service.UserService;

import static com.drychan.handler.MessageHandler.NEXT_LINE;
import static com.drychan.model.ButtonColor.NEGATIVE;
import static com.drychan.model.ButtonColor.SECONDARY;
import static com.drychan.model.Keyboard.TEXT_BUTTON_TYPE;
import static com.drychan.model.Keyboard.startKeyboard;

public enum DefaultCommands {
    HELP("help") {
        @Override
        void processCommand(int userId, MessageSender messageSender, UserService userService,
                            LikeService likeService) {
            messageSender.send(MessageSender.MessageSendQuery.builder()
                    .userId(userId)
                    .message(HELP_MESSAGE)
                    .keyboard(helpKeyboard(true))
                    .build());
        }
    },
    DELETE("delete") {
        @Override
        void processCommand(int userId, MessageSender messageSender, UserService userService,
                            LikeService likeService) {
            userService.deleteById(userId);
            likeService.deleteAllLikesByUser(userId);
            messageSender.send(MessageSender.MessageSendQuery.builder()
                    .userId(userId)
                    .message("Ваш профиль удален, нажмите start, чтобы создать новую анкету")
                    .keyboard(startKeyboard(true))
                    .build());
        }
    };

    private final String command;

    DefaultCommands(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public static final String HELP_MESSAGE = "Список команд:" + NEXT_LINE +
            HELP.getCommand() + ": вывести список команд" + NEXT_LINE +
            DELETE.getCommand() + ": удалить свой аккаунт, связанные лайки также исчезнут";

    abstract void processCommand(int userId, MessageSender messageSender, UserService userService,
                                 LikeService likeService);

    /**
     * @return null if text is not default command
     */
    public static DefaultCommands getCommandFromText(String text) {
        for (var command : DefaultCommands.values()) {
            if (command.getCommand().equals(text)) {
                return command;
            }
        }
        return null;
    }

    public static Keyboard helpKeyboard(boolean inline) {
        Button helpButton = new Button(SECONDARY.getColor(), ButtonAction.builder()
                .type(TEXT_BUTTON_TYPE)
                .label(HELP.getCommand())
                .build());
        Button deleteButton = new Button(NEGATIVE.getColor(), ButtonAction.builder()
                .type(TEXT_BUTTON_TYPE)
                .label(DELETE.getCommand())
                .build());
        Button[][] buttons = {{helpButton, deleteButton}};
        return new Keyboard(false, inline, buttons);
    }
}
