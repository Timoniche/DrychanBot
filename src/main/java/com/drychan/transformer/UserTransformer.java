package com.drychan.transformer;

import com.drychan.dao.model.User;
import com.drychan.dto.UserTo;
import org.springframework.stereotype.Component;

@Component
public class UserTransformer {

    public User transform(UserTo userTo) {
        return User.builder()
                .userId(userTo.getUserId())
                .name(userTo.getName())
                .age(userTo.getAge())
                .gender(userTo.getGender())
                .description(userTo.getDescription())
                .status(User.Status.published)
                .build();
    }
}
