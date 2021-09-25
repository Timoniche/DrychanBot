package com.drychan.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import static com.drychan.handler.DraftUserProcessingStage.NO_VOICE_PATH;

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
    @Id
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "name")
    private String name;

    //todo: to enum
    /**
     * 'm' - male
     * 'f' - female
     */
    @Column(name = "gender")
    private Character gender;

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
        DRAFT("DRAFT"),
        PUBLISHED("PUBLISHED");

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

    public boolean isMale() {
        return gender.equals('m');
    }

    public boolean isFemale() {
        return gender.equals('f');
    }

    public boolean hasVoiceRecord() {
        return isVoiceRecordExist(getVoicePath());
    }

    public static boolean isVoiceRecordExist(String voicePath) {
        return voicePath != null && !voicePath.equals(NO_VOICE_PATH);
    }
}
