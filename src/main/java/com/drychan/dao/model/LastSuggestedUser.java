package com.drychan.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "last_suggested_users")
public class LastSuggestedUser {
    @Id
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "suggested_user_id")
    private Integer suggestedId;
}
