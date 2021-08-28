package com.drychan.controller;

import com.drychan.dao.model.User;
import com.drychan.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/user")
    public String addUser(@RequestParam("user_id") Integer userId,
                          @RequestParam("name") String name,
                          @RequestParam("gender") char gender,
                          @RequestParam(value = "description", required = false, defaultValue = "") String description) {
        User user = User.builder()
                .userId(userId)
                .name(name)
                .gender(gender)
                .description(description)
                .photoPath("")
                .status(User.Status.published)
                .build();
        userService.saveUser(user);
        return "ok";
    }
}
