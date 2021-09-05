package com.drychan.repository;

import com.drychan.dao.model.LastSuggestedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SuggestedRepository extends JpaRepository<LastSuggestedUser, Integer> {
    @Query(value = "SELECT suggested_user_id " +
            "FROM last_suggested_users " +
            "WHERE user_id = :userId ", nativeQuery = true)
    Integer lastSuggestedUserId(int userId);
}
