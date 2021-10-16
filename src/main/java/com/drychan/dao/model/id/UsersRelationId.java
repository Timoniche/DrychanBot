package com.drychan.dao.model.id;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class UsersRelationId implements Serializable {
    private Integer userId;

    private Integer userToId;
}
