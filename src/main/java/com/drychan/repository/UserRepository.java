package com.drychan.repository;

import com.drychan.dao.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    //todo: set published from enum -> eliminate native queries
    @Query(value = "" +
            "SELECT user_id FROM users " +
            "WHERE gender = :gender " +
            "  AND user_id NOT IN (:likedIds) " +
            "  AND status = 'PUBLISHED' " +
            "ORDER BY RANDOM() " +
            "LIMIT 1 ",
            nativeQuery = true)
    Integer findRandomNotLikedByUserWithGender(@Param("gender") char gender,
                                               @Param("likedIds") Iterable<Integer> likedIds);
}
