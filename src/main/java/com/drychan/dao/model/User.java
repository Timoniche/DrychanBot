package com.drychan.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.vk.api.sdk.objects.base.Sex;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import static com.drychan.handler.DraftUserProcessor.NO_VOICE_PATH;
import static com.drychan.dao.model.User.Gender.MALE;
import static com.drychan.dao.model.User.Gender.FEMALE;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TypeDef(
        name = "pgsql_enum",
        typeClass = PostgreSQLEnumType.class
)
@Table(name = "users")
public class User {
    public static final String DRAFT_DB = "DRAFT";
    public static final String PUBLISHED_DB = "PUBLISHED";
    public static final String LOVE_LETTER_DB = "LOVE_LETTER_DB";

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(name = "gender")
    private Gender gender;

    @Column(name = "age")
    private Integer age;

    @Column(name = "description")
    private String description;

    @Column(name = "photo_path")
    private String photoPath;

    @Column(name = "voice_path")
    private String voicePath;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(name = "status")
    private Status status;

    public enum Status {
        DRAFT(DRAFT_DB),
        LOVE_LETTER(LOVE_LETTER_DB),
        PUBLISHED(PUBLISHED_DB);

        private final String visibility;

        Status(String visibility) {
            this.visibility = visibility;
        }

        public String getVisibility() {
            return visibility;
        }

        public static Status getStatusFromDb(String visibility) {
            for (Status status : Status.values()) {
                if (status.getVisibility().equals(visibility)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unsupported visibility status: " + visibility);
        }

        public static String statusToDb(Status status) {
            return status.getVisibility();
        }
    }

    public enum Gender {
        MALE("MALE"),
        FEMALE("FEMALE");

        private final String sex;

        Gender(String sex) {
            this.sex = sex;
        }

        public String getSex() {
            return sex;
        }

        public static Gender getGenderFromDb(String sex) {
            for (Gender gender : Gender.values()) {
                if (gender.getSex().equals(sex)) {
                    return gender;
                }
            }
            throw new IllegalArgumentException("Unsupported gender: " + sex);
        }

        public static String genderToDb(Gender gender) {
            return gender.getSex();
        }

        /**
         * @return null for unknown gender
         */
        public static Gender genderFromVkSex(Sex sex) {
            switch (sex) {
                case MALE:
                    return MALE;
                case FEMALE:
                    return FEMALE;
                case UNKNOWN:
                default:
                    return null;
            }
        }
    }

    public boolean isFemale() {
        return getGender() == FEMALE;
    }

    public boolean isMale() {
        return getGender() == MALE;
    }

    public boolean isDraft() {
        return getStatus() == Status.DRAFT;
    }

    public boolean isActive() {
        return !isDraft();
    }

    public boolean hasVoiceRecord() {
        return isVoiceRecordExist(getVoicePath());
    }

    public static boolean isVoiceRecordExist(String voicePath) {
        return voicePath != null && !voicePath.equals(NO_VOICE_PATH);
    }
}
