package com.drychan.service;

import java.util.Optional;

import com.drychan.model.User;
import com.drychan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public void saveUser(User user) {
        userRepository.save(user);
    }

    public Optional<User> findById(int userId) {
        return userRepository.findById(userId);
    }
}
