package com.drychan.dao.model.id;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class LikeId implements Serializable {
    private Integer userFrom;

    private Integer userTo;
}
