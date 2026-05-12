-- roles table
CREATE TABLE roles (
                       id   SERIAL PRIMARY KEY,
                       name VARCHAR(50) NOT NULL UNIQUE
);
-- users table
CREATE TABLE users (
                       id       UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
                       username VARCHAR(255) NOT NULL UNIQUE,
                       email    VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL
);
-- user_roles join table
CREATE TABLE user_roles (
                            user_id UUID    NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
                            role_id INTEGER NOT NULL REFERENCES roles(id)  ON DELETE CASCADE,
                            PRIMARY KEY (user_id, role_id)
);
-- refresh_tokens table
CREATE TABLE refresh_tokens (
                                id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
                                expiry_date TIMESTAMP   NOT NULL,
                                revoked     BOOLEAN     NOT NULL DEFAULT FALSE,
                                user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE
);

-- create roles
INSERT INTO roles (name) VALUES
                             ('ROLE_USER'),
                             ('ROLE_ADMIN'),
                             ('ROLE_OWNER');