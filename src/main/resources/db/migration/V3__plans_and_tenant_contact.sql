-- Add contact email to tenants
ALTER TABLE tenants ADD COLUMN contact_email VARCHAR(255);

-- Subscription plans
CREATE TABLE plans (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(100) NOT NULL UNIQUE,
    description             TEXT,
    price                   NUMERIC(10, 2) NOT NULL DEFAULT 0,
    billing_cycle           VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    max_merchants           INT,
    max_users_per_merchant  INT,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_plans_active ON plans(is_active);

-- Default plans
INSERT INTO plans (name, description, price, billing_cycle, max_merchants, max_users_per_merchant)
VALUES
    ('FREE',       'Plan gratuit — démarrage',      0,    'MONTHLY',  1,  5),
    ('STARTER',    'Petites entreprises',            29,   'MONTHLY',  5,  20),
    ('BUSINESS',   'Croissance accélérée',           99,   'MONTHLY',  20, 100),
    ('ENTERPRISE', 'Grandes structures, sur mesure', 299,  'MONTHLY',  NULL, NULL);
