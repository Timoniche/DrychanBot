package com.drychan.repository;

import java.util.List;

import com.drychan.model.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LikeRepository extends JpaRepository<Like, Integer> {

    @Query(value = "SELECT user_to FROM likes WHERE user_from = :userId", nativeQuery = true)
    List<Integer> findLikedByUser(int userId);

}
