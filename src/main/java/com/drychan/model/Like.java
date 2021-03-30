package com.drychan.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

import com.drychan.model.id.LikeId;
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
@IdClass(LikeId.class)
@Table(name = "likes")
public class Like {
    @Id
    @Column(name = "user_from")
    private Integer userFrom;

    @Id
    @Column(name = "user_to")
    private Integer userTo;
}
