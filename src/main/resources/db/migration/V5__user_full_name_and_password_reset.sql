-- Add full_name to tenant_users and merchant_users
ALTER TABLE tenant_users  ADD COLUMN full_name VARCHAR(255);
ALTER TABLE merchant_users ADD COLUMN full_name VARCHAR(255);

-- Password reset tokens
CREATE TABLE password_reset_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email      VARCHAR(255) NOT NULL,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_reset_tokens_token ON password_reset_tokens(token);
CREATE INDEX idx_reset_tokens_email ON password_reset_tokens(email);
