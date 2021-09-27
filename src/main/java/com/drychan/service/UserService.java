package com.drychan.service;

import java.util.List;
import java.util.Optional;

import com.drychan.dao.model.User;
import com.drychan.repository.LikeRepository;
import com.drychan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.drychan.dao.model.User.Gender;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;

    public void saveUser(User user) {
        userRepository.save(user);
    }

    public Optional<User> findById(int userId) {
        return userRepository.findById(userId);
    }

    public void deleteById(int userId) {
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
        }
    }

    public Integer findRandomNotLikedByUserWithGender(int userId, Gender gender) {
        List<Integer> likedByUser = likeRepository.findLikedByUser(userId);
        likedByUser.add(userId);
        return userRepository.findRandomNotLikedByUserWithGender(gender.getSex(), likedByUser);
    }
}
