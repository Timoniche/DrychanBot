package com.drychan.controller;

import com.drychan.dao.model.User;
import com.drychan.dto.UserTo;
import com.drychan.service.UserService;
import com.drychan.transformer.UserTransformer;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    private static final String OK_BODY = "ok";

    private final UserService userService;
    private final UserTransformer userTransformer;

    public UserController(UserService userService, UserTransformer userTransformer) {
        this.userService = userService;
        this.userTransformer = userTransformer;
    }

    @PostMapping("/user")
    public String addUser(@RequestBody UserTo userTo) {
        User user = userTransformer.transform(userTo);
        userService.saveUser(user);
        return OK_BODY;
    }

    @DeleteMapping("/user/{userId}")
    public String removeUser(@PathVariable int userId) {
        userService.deleteById(userId);
        return OK_BODY;
    }
}
