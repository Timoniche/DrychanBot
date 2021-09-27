package com.drychan.handler;

import com.drychan.dao.model.User;
import com.drychan.model.Button;
import com.drychan.model.ButtonColor;
import com.drychan.model.Keyboard;
import com.drychan.service.LikeService;
import com.drychan.service.UserService;
import lombok.extern.log4j.Log4j2;

import static com.drychan.dao.model.User.Status.DRAFT;
import static com.drychan.model.Keyboard.START_LABEL;
import static com.drychan.model.Keyboard.buttonOf;
import static com.drychan.model.Keyboard.deleteButton;
import static com.drychan.model.Keyboard.startKeyboard;

@Log4j2
public enum DefaultCommands {
    HELP("помощь") {
        @Override
        void processCommand(int userId, MessageSender messageSender, UserService userService,
                            LikeService likeService, DraftUserProcessor draftUserProcessor) {
            messageSender.send(MessageSender.MessageSendQuery.builder()
                    .userId(userId)
                    .message(COMMANDS_LIST)
                    .keyboard(commandsKeyboard(true))
                    .build());
        }
    },
    CHANGE_NAME("изменить имя") {
        @Override
        void processCommand(int userId, MessageSender messageSender, UserService userService,
                            LikeService likeService, DraftUserProcessor draftUserProcessor) {
            User user = userService.findById(userId).orElse(null);
            if (user == null) {
                return;
            }
            user.setName(null);
            user.setStatus(DRAFT);
            userService.saveUser(user);
            log.info("userId={} set name to null, now user is draft", userId);
            draftUserProcessor.askQuestionForNextStage(user);
        }
    },
    CHANGE_AGE("изменить возраст") {
        @Override
        void processCommand(int userId, MessageSender messageSender, UserService userService,
                            LikeService likeService, DraftUserProcessor draftUserProcessor) {
            User user = userService.findById(userId).orElse(null);
            if (user == null) {
                return;
            }
            user.setAge(null);
            user.setStatus(DRAFT);
            userService.saveUser(user);
            log.info("userId={} set age to null, now user is draft", userId);
            draftUserProcessor.askQuestionForNextStage(user);
        }
    },
    CHANGE_DESCRIPTION("изменить описание") {
        @Override
        void processCommand(int userId, MessageSender messageSender, UserService userService,
                            LikeService likeService, DraftUserProcessor draftUserProcessor) {
            User user = userService.findById(userId).orElse(null);
            if (user == null) {
                return;
            }
            user.setDescription(null);
            user.setStatus(DRAFT);
            userService.saveUser(user);
            log.info("userId={} set description to null, now user is draft", userId);
            draftUserProcessor.askQuestionForNextStage(user);
        }
    },
    CHANGE_PHOTO("изменить фото") {
        @Override
        void processCommand(int userId, MessageSender messageSender, UserService userService,
                            LikeService likeService, DraftUserProcessor draftUserProcessor) {
            User user = userService.findById(userId).orElse(null);
            if (user == null) {
                return;
            }
            user.setPhotoPath(null);
            user.setStatus(DRAFT);
            userService.saveUser(user);
            log.info("userId={} set photo to null, now user is draft", userId);
            draftUserProcessor.askQuestionForNextStage(user);
        }
    },
    CHANGE_VOICE("изменить голосовое") {
        @Override
        void processCommand(int userId, MessageSender messageSender, UserService userService,
                            LikeService likeService, DraftUserProcessor draftUserProcessor) {
            User user = userService.findById(userId).orElse(null);
            if (user == null) {
                return;
            }
            user.setVoicePath(null);
            user.setStatus(DRAFT);
            userService.saveUser(user);
            log.info("userId={} set voice to null, now user is draft", userId);
            draftUserProcessor.askQuestionForNextStage(user);
        }
    },
    DELETE("удалить аккаунт") {
        @Override
        void processCommand(int userId, MessageSender messageSender, UserService userService,
                            LikeService likeService, DraftUserProcessor draftUserProcessor) {
            userService.deleteById(userId);
            likeService.deleteAllLikesByUser(userId);
            messageSender.send(MessageSender.MessageSendQuery.builder()
                    .userId(userId)
                    .message("Профиль удален, нажми " + START_LABEL + ", чтобы создать новую анкету")
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

    public static final String COMMANDS_LIST = "Список команд:";

    public static Keyboard commandsKeyboard(boolean inline) {
        Button changeNameButton = buttonOf(ButtonColor.SECONDARY, CHANGE_NAME.getCommand());
        Button changeAgeButton = buttonOf(ButtonColor.SECONDARY, CHANGE_AGE.getCommand());
        Button changeDescriptionButton = buttonOf(ButtonColor.SECONDARY, CHANGE_DESCRIPTION.getCommand());
        Button changePhotoButton = buttonOf(ButtonColor.SECONDARY, CHANGE_PHOTO.getCommand());
        Button changeVoiceButton = buttonOf(ButtonColor.SECONDARY, CHANGE_VOICE.getCommand());

        Button[][] buttons = {
                {changeNameButton},
                {changeAgeButton},
                {changeDescriptionButton},
                {changePhotoButton},
                {changeVoiceButton},
                {deleteButton}
        };
        return new Keyboard(false, inline, buttons);
    }

    abstract void processCommand(int userId, MessageSender messageSender, UserService userService,
                                 LikeService likeService, DraftUserProcessor draftUserProcessor);

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
