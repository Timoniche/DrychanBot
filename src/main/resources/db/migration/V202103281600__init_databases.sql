CREATE TYPE USER_STATUS AS ENUM ('draft', 'published');

CREATE TABLE users
(
    user_id INT NOT NULL PRIMARY KEY,
    name TEXT,
    gender CHAR(1) CONSTRAINT gender_check CHECK (gender IN ('m', 'f')),
    age INT,
    description TEXT,
    photo_path TEXT,
    status USER_STATUS NOT NULL
);

CREATE TABLE likes
(
    user_from INT NOT NULL,
    user_to INT NOT NULL,
    PRIMARY KEY (user_from, user_to)
)