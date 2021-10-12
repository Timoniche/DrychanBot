package com.drychan.handler.strategy;

import com.drychan.handler.MessageSender;
import com.drychan.service.UserService;
import lombok.Builder;

import static com.drychan.handler.DefaultCommandsProcessor.COMMANDS_LIST;
import static com.drychan.handler.DefaultCommandsProcessor.commandsKeyboard;

public class HelpStrategy extends BasicCommandStrategy {
    @Builder
    public HelpStrategy(MessageSender messageSender, UserService userService) {
        super(messageSender, userService);
    }

    @Override
    public boolean process(int userId) {
        messageSender.send(MessageSender.MessageSendQuery.builder()
                .userId(userId)
                .message(COMMANDS_LIST)
                .keyboard(commandsKeyboard(true))
                .build());
        return true;
    }
}
