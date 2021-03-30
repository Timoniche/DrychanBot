CREATE TABLE users
(
    user_id INT NOT NULL PRIMARY KEY,
    name TEXT,
    gender CHAR(1) CONSTRAINT gender_check CHECK (gender IN ('m', 'f')),
    description TEXT,
    photo_path TEXT
);

CREATE TABLE likes
(
    user_from INT NOT NULL,
    user_to INT NOT NULL,
    PRIMARY KEY (user_from, user_to)
)