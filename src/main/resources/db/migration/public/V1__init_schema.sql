-- Platform Users
CREATE TABLE platform_users (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                username VARCHAR(32) NOT NULL UNIQUE,
                                email VARCHAR(255) NOT NULL UNIQUE,
                                password VARCHAR(255) NOT NULL,
                                role VARCHAR(32) NOT NULL,
                                created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Tenants
CREATE TABLE tenants (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         company_name VARCHAR(255) NOT NULL,
                         company_code VARCHAR(32) NOT NULL UNIQUE,
                         schema_name VARCHAR(255) UNIQUE,
                         status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
                         owner_email VARCHAR(255),
                         created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                         updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Platform Refresh Tokens
CREATE TABLE platform_refresh_tokens (
                                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                         expiry_date TIMESTAMP NOT NULL,
                                         revoked BOOLEAN NOT NULL DEFAULT FALSE,
                                         user_id UUID NOT NULL REFERENCES platform_users(id) ON DELETE CASCADE
);

-- Seed Platform Owner
INSERT INTO platform_users (username, email, password, role)
VALUES (
           'owner',
           'owner@securevault.com',
           '$2a$12$JbtwBQiLp9uDRErXWPZjneZzHCCSwykRe70pOV2k7aNBpej6KyQVW',
           'PLATFORM_OWNER'
       );