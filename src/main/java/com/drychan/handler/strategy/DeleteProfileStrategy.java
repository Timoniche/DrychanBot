package com.drychan.handler.strategy;

import com.drychan.handler.MessageSender;
import com.drychan.service.UsersRelationService;
import com.drychan.service.UserService;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import static com.drychan.model.Keyboard.START_LABEL;
import static com.drychan.model.Keyboard.startKeyboard;

@Log4j2
public class DeleteProfileStrategy extends BasicCommandStrategy {
    private final UsersRelationService usersRelationService;

    @Builder
    public DeleteProfileStrategy(MessageSender messageSender, UserService userService,
                                 UsersRelationService usersRelationService) {
        super(messageSender, userService);
        this.usersRelationService = usersRelationService;
    }

    @Override
    public boolean process(int userId) {
        userService.deleteById(userId);
        usersRelationService.deleteAllVotesByUser(userId);
        messageSender.send(MessageSender.MessageSendQuery.builder()
                .userId(userId)
                .message("Профиль удален, нажми " + START_LABEL + ", чтобы создать новую анкету")
                .keyboard(startKeyboard(true))
                .build());
        return true;
    }
}
