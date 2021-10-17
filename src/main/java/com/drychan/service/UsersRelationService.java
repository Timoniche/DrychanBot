package com.drychan.service;

import java.util.List;
import java.util.Optional;

import com.drychan.dao.model.UsersRelation;
import com.drychan.dao.model.id.UsersRelationId;
import com.drychan.repository.UsersRelationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.drychan.dao.model.UsersRelation.Vote.DISLIKE;
import static com.drychan.dao.model.UsersRelation.Vote.LIKE;

@Service
@RequiredArgsConstructor
public class UsersRelationService {
    private final UsersRelationRepository usersRelationRepository;

    public void putLike(int userId, int userToId) {
        usersRelationRepository.save(new UsersRelation(userId, userToId, LIKE));
    }

    public void putDislike(int userId, int userToId) {
        usersRelationRepository.save(new UsersRelation(userId, userToId, DISLIKE));
    }

    public Optional<UsersRelation> findById(UsersRelationId usersRelationId) {
        return usersRelationRepository.findById(usersRelationId);
    }

    public boolean isLikeExistsById(UsersRelationId usersRelationId) {
        UsersRelation usersRelation = findById(usersRelationId).orElse(null);
        return usersRelation != null && usersRelation.getVote() == LIKE;
    }

    public List<UsersRelation> findDislikedByUsed(int userId) {
        return usersRelationRepository.findDislikedByUser(userId);
    }

    public void deleteDislikeVotesByUser(int userId) {
        usersRelationRepository.deleteDislikeVotesByUser(userId);
    }

    public void deleteAllVotesConnectedWithUser(int userId) {
        usersRelationRepository.deleteAllVotesConnectedWithUser(userId);
    }
}
