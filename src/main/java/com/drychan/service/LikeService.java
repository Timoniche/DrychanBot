package com.drychan.service;

import com.drychan.dao.model.Like;
import com.drychan.dao.model.id.LikeId;
import com.drychan.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LikeService {
    private final LikeRepository likeRepository;

    public void putLike(Like like) {
        likeRepository.save(like);
    }

    public boolean isLikeExists(Like like) {
        return likeRepository.findById(new LikeId(like.getUserFrom(), like.getUserTo())).isPresent();
    }

    public void deleteAllLikesByUser(int userId) {
        likeRepository.deleteAllLikesByUser(userId);
    }
}
