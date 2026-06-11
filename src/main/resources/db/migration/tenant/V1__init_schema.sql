-- Tenant users
CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       username VARCHAR(32) NOT NULL UNIQUE,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       role VARCHAR(32) NOT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Tenant refresh tokens
CREATE TABLE refresh_tokens (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                expiry_date TIMESTAMP NOT NULL,
                                revoked BOOLEAN NOT NULL DEFAULT FALSE,
                                user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE
);

-- Invite codes
CREATE TABLE invite_codes (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              code VARCHAR(255) NOT NULL UNIQUE,
                              role VARCHAR(32) NOT NULL DEFAULT 'TENANT_MEMBER',
                              created_by UUID NOT NULL REFERENCES users(id),
                              invitee_email VARCHAR(255) NOT NULL,
                              expires_at TIMESTAMP NOT NULL,
                              used_at TIMESTAMP,
                              created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                              updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);