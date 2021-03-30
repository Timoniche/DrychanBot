package com.drychan.service;

import com.drychan.model.Like;
import com.drychan.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeService {
    private final LikeRepository likeRepository;

    @Transactional
    public void putLike(Like like) {
        likeRepository.save(like);
    }
}