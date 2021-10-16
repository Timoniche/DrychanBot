package com.drychan.handler.strategy.changing;

import com.drychan.dao.model.User;
import com.drychan.handler.DraftUserProcessor;
import com.drychan.handler.MessageSender;
import com.drychan.handler.strategy.BasicCommandStrategy;
import com.drychan.service.UserService;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import static com.drychan.dao.model.User.Status.DRAFT;

@Log4j2
public class ChangePhotoStrategy extends BasicCommandStrategy {
    private final DraftUserProcessor draftUserProcessor;

    @Builder
    public ChangePhotoStrategy(MessageSender messageSender, UserService userService,
                                     DraftUserProcessor draftUserProcessor) {
        super(messageSender, userService);
        this.draftUserProcessor = draftUserProcessor;
    }

    @Override
    public boolean process(int userId) {
        User user = userService.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        user.setPhotoPath(null);
        user.setStatus(DRAFT);
        userService.saveUser(user);
        log.info("userId={} set photo to null, now user is draft", userId);
        draftUserProcessor.askQuestionForNextStage(user);
        return true;
    }
}
