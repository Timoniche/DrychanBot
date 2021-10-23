package com.drychan.repository;

import java.util.List;

import com.drychan.dao.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import static com.drychan.dao.model.User.Gender;
import static com.drychan.dao.model.User.PUBLISHED_DB;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    @Query(value = "" +
            " from User as user " +
            " where user.gender = :gender " +
            "   and user.status = '" + PUBLISHED_DB + "'" +
            "   and user.userId not in (:votedAndOwnIds) " +
            " order by random()"
    )
    List<User> findNotVotedByUserWithGender(
            @Param("votedAndOwnIds") List<Integer> votedAndOwnIds,
            @Param("gender") Gender gender,
            Pageable pageable
    );
}
