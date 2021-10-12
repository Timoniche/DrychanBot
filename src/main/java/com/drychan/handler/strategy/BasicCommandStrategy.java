package com.drychan.handler.strategy;

import com.drychan.handler.MessageSender;
import com.drychan.service.UserService;

public abstract class BasicCommandStrategy implements CommandStrategy {
    protected final MessageSender messageSender;
    protected final UserService userService;

    public BasicCommandStrategy(MessageSender messageSender, UserService userService) {
        this.messageSender = messageSender;
        this.userService = userService;
    }
}
