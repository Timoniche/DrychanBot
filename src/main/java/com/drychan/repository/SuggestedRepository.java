package com.drychan.repository;

import java.util.List;

import com.drychan.dao.model.LastSuggestedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SuggestedRepository extends JpaRepository<LastSuggestedUser, Integer> {
    @Query(value = "" +
            " from LastSuggestedUser su " +
            " where su.userId = :userId "
    )
    List<LastSuggestedUser> lastSuggestedUsers(@Param("userId") int userId);
}
