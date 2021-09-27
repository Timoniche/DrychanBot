package com.drychan.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Data;

import static com.drychan.dao.model.User.Gender;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Data
public class UserTo {
    private Integer userId;
    private String name;
    private Gender gender;
    private Integer age;
    private String description;
    private String photoPath;
}
