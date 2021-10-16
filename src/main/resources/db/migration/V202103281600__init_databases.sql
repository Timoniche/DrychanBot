CREATE TYPE USER_STATUS AS ENUM('DRAFT', 'PUBLISHED');

CREATE TYPE USER_SEX AS ENUM('MALE', 'FEMALE');

CREATE TABLE users
(
    user_id INT NOT NULL PRIMARY KEY,
    name TEXT,
    gender USER_SEX,
    age INT,
    description TEXT,
    photo_path TEXT,
    voice_path TEXT,
    status USER_STATUS NOT NULL
);

CREATE TYPE USER_VOTE AS ENUM('LIKE', 'DISLIKE');

CREATE TABLE users_relation
(
    user_id INT NOT NULL,
    user_to_id INT NOT NULL,
    vote USER_VOTE NOT NULL,
    PRIMARY KEY (user_id, user_to_id)
);

CREATE TABLE last_suggested_users
(
    user_id INT NOT NULL,
    suggested_user_id INT NOT NULL,
    PRIMARY KEY (user_id)
);