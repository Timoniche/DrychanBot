package com.drychan.controller;

import com.drychan.model.User;
import com.drychan.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/users")
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/addUser")
    public String addUser(@RequestParam("desc") String description) {
        User user = new User(description);
        userService.addUser(user);
        return "ok";
    }
}
