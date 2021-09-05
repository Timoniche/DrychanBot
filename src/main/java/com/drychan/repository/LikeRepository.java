package com.drychan.repository;

import java.util.List;

import com.drychan.dao.model.Like;
import com.drychan.dao.model.id.LikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface LikeRepository extends JpaRepository<Like, LikeId> {

    @Query(value = "SELECT user_to FROM likes WHERE user_from = :userId", nativeQuery = true)
    List<Integer> findLikedByUser(int userId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM likes " +
            "WHERE user_from = :userId " +
            "   OR user_to = :userId ", nativeQuery = true)
    void deleteAllLikesByUser(int userId);
}
