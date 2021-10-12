package com.drychan.handler.strategy;

import com.drychan.handler.MessageSender;
import com.drychan.service.LikeService;
import com.drychan.service.UserService;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import static com.drychan.model.Keyboard.START_LABEL;
import static com.drychan.model.Keyboard.startKeyboard;

@Log4j2
public class DeleteProfileStrategy extends BasicCommandStrategy {
    private final LikeService likeService;

    @Builder
    public DeleteProfileStrategy(MessageSender messageSender, UserService userService,
                                 LikeService likeService) {
        super(messageSender, userService);
        this.likeService = likeService;
    }

    @Override
    public boolean process(int userId) {
        userService.deleteById(userId);
        likeService.deleteAllLikesByUser(userId);
        messageSender.send(MessageSender.MessageSendQuery.builder()
                .userId(userId)
                .message("Профиль удален, нажми " + START_LABEL + ", чтобы создать новую анкету")
                .keyboard(startKeyboard(true))
                .build());
        return true;
    }
}
