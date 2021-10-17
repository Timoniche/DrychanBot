package com.drychan.repository;

import java.util.List;

import com.drychan.dao.model.UsersRelation;
import com.drychan.dao.model.id.UsersRelationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UsersRelationRepository extends JpaRepository<UsersRelation, UsersRelationId> {

    @Query(value = "" +
            " from UsersRelation as ur " +
            " where ur.userId = :userId "
    )
    List<UsersRelation> votedUsersByUserId(@Param("userId") int userId);

    @Modifying
    @Transactional
    @Query(value = "" +
            " delete from UsersRelation as ur " +
            " where ur.userId = :userId " +
            "    or ur.userToId = :userId "
    )
    void deleteAllVotesByUser(@Param("userId") int userId);
}
