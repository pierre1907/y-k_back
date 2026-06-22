-- Merchant actif sélectionné par un tenant_user (sélecteur merchant côté front).
ALTER TABLE tenant_users ADD COLUMN active_merchant_id UUID REFERENCES merchants(id) ON DELETE SET NULL;
CREATE INDEX idx_tenant_users_active_merchant ON tenant_users(active_merchant_id);
