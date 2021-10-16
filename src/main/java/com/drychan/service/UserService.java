package com.drychan.service;

import java.util.List;
import java.util.Optional;

import com.drychan.dao.model.User;
import com.drychan.dao.model.UsersRelation;
import com.drychan.repository.UsersRelationRepository;
import com.drychan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import static com.drychan.dao.model.User.Gender;
import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UsersRelationRepository usersRelationRepository;

    private static final Pageable LIMIT_1 = PageRequest.of(0, 1);

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

    public Optional<User> findRandomNotLikedByUserWithGender(int userId, Gender gender) {
        List<Integer> votedUsersByUserAndOwnId = usersRelationRepository.votedUsersByUserId(userId).stream()
                .map(UsersRelation::getUserToId)
                .collect(toList());
        votedUsersByUserAndOwnId.add(userId);
        List<User> profileToSuggest = userRepository.findNotVotedByUserWithGender(
                votedUsersByUserAndOwnId,
                gender,
                LIMIT_1
        );
        return profileToSuggest.isEmpty() ? Optional.empty() : Optional.of(profileToSuggest.get(0));
    }
}
