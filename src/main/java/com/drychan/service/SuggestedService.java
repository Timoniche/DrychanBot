package com.drychan.service;

import java.util.List;
import java.util.Optional;

import com.drychan.dao.model.LastSuggestedUser;
import com.drychan.repository.SuggestedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SuggestedService {
    private final SuggestedRepository suggestedRepository;

    public void saveLastSuggested(int userId, int suggestedUserId) {
        suggestedRepository.save(new LastSuggestedUser(userId, suggestedUserId));
    }

    public Optional<LastSuggestedUser> lastSuggestedUser(int userId) {
        List<LastSuggestedUser> lastSuggestedUsers = suggestedRepository.lastSuggestedUsers(userId);
        return lastSuggestedUsers.isEmpty() ? Optional.empty() : Optional.of(lastSuggestedUsers.get(0));
    }
}
