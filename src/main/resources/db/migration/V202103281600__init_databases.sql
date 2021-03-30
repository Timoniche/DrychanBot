CREATE TABLE users
(
    user_id INT NOT NULL PRIMARY KEY,
    name TEXT,
    gender CHAR(1) CONSTRAINT gender_check CHECK (gender IN ('m', 'f')),
    description TEXT
);
