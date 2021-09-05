package com.drychan.service;

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

    public Integer lastSuggestedUserId(int userId) {
        return suggestedRepository.lastSuggestedUserId(userId);
    }
}
