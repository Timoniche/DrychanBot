CREATE TYPE USER_STATUS AS ENUM ('DRAFT', 'PUBLISHED');

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

CREATE TABLE likes
(
    user_from INT NOT NULL,
    user_to INT NOT NULL,
    PRIMARY KEY (user_from, user_to)
);

CREATE TABLE last_suggested_users
(
    user_id INT NOT NULL,
    suggested_user_id INT NOT NULL,
    PRIMARY KEY (user_id)
);