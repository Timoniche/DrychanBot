package com.drychan.dao.model.id;

import java.io.Serializable;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class UsersRelationId implements Serializable {
    private Integer userId;

    private Integer userToId;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UsersRelationId that = (UsersRelationId) o;
        return userId.equals(that.userId) && userToId.equals(that.userToId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, userToId);
    }
}
