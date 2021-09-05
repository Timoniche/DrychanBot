package com.drychan.handler;

import com.drychan.service.UserService;

import static com.drychan.handler.MessageHandler.NEXT_LINE;
import static com.drychan.model.Keyboard.helpKeyboard;

public enum DefaultCommands {
    HELP("help") {
        @Override
        void processCommand(int userId, MessageSender messageSender, UserService userService) {
            messageSender.send(userId, HELP_MESSAGE, null, helpKeyboard(true));
        }
    },
    DELETE("delete") {
        @Override
        void processCommand(int userId, MessageSender messageSender, UserService userService) {
            messageSender.send(userId, "Ваш профиль удален, напишите start, чтобы создать новую анкету");
            userService.deleteById(userId);
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
            DELETE.getCommand() + ": удалить свой аккаунт";

    abstract void processCommand(int userId, MessageSender messageSender, UserService userService);

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
}
