package com.drychan.handler;

import java.util.Map;
import java.util.Optional;

import com.drychan.handler.strategy.changing.ChangeAgeStrategy;
import com.drychan.handler.strategy.changing.ChangeDescriptionStrategy;
import com.drychan.handler.strategy.changing.ChangeNameStrategy;
import com.drychan.handler.strategy.changing.ChangePhotoStrategy;
import com.drychan.handler.strategy.changing.ChangeVoiceStrategy;
import com.drychan.handler.strategy.CommandStrategy;
import com.drychan.handler.strategy.DeleteProfileStrategy;
import com.drychan.handler.strategy.HelpStrategy;
import com.drychan.model.Button;
import com.drychan.model.ButtonColor;
import com.drychan.model.Keyboard;
import com.drychan.service.UsersRelationService;
import com.drychan.service.UserService;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import static com.drychan.handler.DefaultCommandsProcessor.DefaultCommands.CHANGE_AGE;
import static com.drychan.handler.DefaultCommandsProcessor.DefaultCommands.CHANGE_DESCRIPTION;
import static com.drychan.handler.DefaultCommandsProcessor.DefaultCommands.CHANGE_NAME;
import static com.drychan.handler.DefaultCommandsProcessor.DefaultCommands.CHANGE_PHOTO;
import static com.drychan.handler.DefaultCommandsProcessor.DefaultCommands.CHANGE_VOICE;
import static com.drychan.handler.DefaultCommandsProcessor.DefaultCommands.DELETE;
import static com.drychan.handler.DefaultCommandsProcessor.DefaultCommands.HELP;
import static com.drychan.model.Keyboard.buttonOf;
import static com.drychan.model.Keyboard.deleteButton;

@Log4j2
public class DefaultCommandsProcessor {
    public static final String COMMANDS_LIST = "Список команд:";

    private final Map<DefaultCommands, CommandStrategy> commandToStrategyMap;

    @Builder
    public DefaultCommandsProcessor(MessageSender messageSender, UserService userService,
                                    DraftUserProcessor draftUserProcessor, UsersRelationService usersRelationService) {
        commandToStrategyMap = buildCommandToStrategyMap(
                messageSender,
                userService,
                draftUserProcessor,
                usersRelationService
        );
    }

    public void process(int userId, CommandStrategy commandStrategy) {
        commandStrategy.process(userId);
    }

    public enum DefaultCommands {
        HELP("помощь"),
        CHANGE_NAME("изменить имя"),
        CHANGE_AGE("изменить возраст"),
        CHANGE_DESCRIPTION("изменить описание"),
        CHANGE_PHOTO("изменить фото"),
        CHANGE_VOICE("изменить голосовое"),
        DELETE("удалить аккаунт");

        private final String command;

        DefaultCommands(String command) {
            this.command = command;
        }

        public String getCommand() {
            return command;
        }

        /**
         * @return null if text is not default command
         */
        public static DefaultCommands getDefaultCommandFromText(String text) {
            for (var command : DefaultCommands.values()) {
                if (command.getCommand().equals(text)) {
                    return command;
                }
            }
            return null;
        }
    }

    public CommandStrategy chooseStrategyFromCommand(DefaultCommands command) {
        return Optional.ofNullable(commandToStrategyMap.get(command))
                .orElseThrow(() -> new IllegalArgumentException("Unsupported default command: " + command));
    }

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

    private Map<DefaultCommands, CommandStrategy> buildCommandToStrategyMap(MessageSender messageSender,
                                                                            UserService userService,
                                                                            DraftUserProcessor draftUserProcessor,
                                                                            UsersRelationService usersRelationService) {
        var helpStrategy = HelpStrategy.builder()
                .userService(userService)
                .messageSender(messageSender)
                .build();
        var changeNameStrategy = ChangeNameStrategy.builder()
                .userService(userService)
                .messageSender(messageSender)
                .draftUserProcessor(draftUserProcessor)
                .build();
        var changeAgeStrategy = ChangeAgeStrategy.builder()
                .userService(userService)
                .messageSender(messageSender)
                .draftUserProcessor(draftUserProcessor)
                .build();
        var changeDescriptionStrategy = ChangeDescriptionStrategy.builder()
                .userService(userService)
                .messageSender(messageSender)
                .draftUserProcessor(draftUserProcessor)
                .build();
        var changePhotoStrategy = ChangePhotoStrategy.builder()
                .userService(userService)
                .messageSender(messageSender)
                .draftUserProcessor(draftUserProcessor)
                .build();
        var changeVoiceStrategy = ChangeVoiceStrategy.builder()
                .userService(userService)
                .messageSender(messageSender)
                .draftUserProcessor(draftUserProcessor)
                .build();
        var deleteProfileStrategy = DeleteProfileStrategy.builder()
                .userService(userService)
                .messageSender(messageSender)
                .usersRelationService(usersRelationService)
                .build();
        return Map.of(
                HELP, helpStrategy,
                CHANGE_NAME, changeNameStrategy,
                CHANGE_AGE, changeAgeStrategy,
                CHANGE_DESCRIPTION, changeDescriptionStrategy,
                CHANGE_PHOTO, changePhotoStrategy,
                CHANGE_VOICE, changeVoiceStrategy,
                DELETE, deleteProfileStrategy
        );
    }
}
