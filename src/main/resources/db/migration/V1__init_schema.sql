CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       username VARCHAR(255) NOT NULL UNIQUE,
                       email VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE user_roles (
    user_id   UUID NOT NULL REFERENCES users(id),
    role_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role_name)
);