package com.drychan.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

import com.drychan.dao.model.id.UsersRelationId;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@IdClass(UsersRelationId.class)
@TypeDef(
        name = "pgsql_enum",
        typeClass = PostgreSQLEnumType.class
)
@Table(name = "users_relation")
public class UsersRelation {
    @Id
    @Column(name = "user_id")
    private Integer userId;

    @Id
    @Column(name = "user_to_id")
    private Integer userToId;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(name = "vote")
    private Vote vote;

    public enum Vote {
        LIKE("LIKE"),
        DISLIKE("DISLIKE");

        private final String vote;

        Vote(String vote) {
            this.vote = vote;
        }

        public String getVote() {
            return vote;
        }
    }

}
