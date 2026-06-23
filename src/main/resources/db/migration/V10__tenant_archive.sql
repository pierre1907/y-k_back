-- Archivage logique d'un tenant (déplacement vers le répertoire des archivés)
-- sans suppression de données. La suppression définitive reste un DELETE
-- explicite, qui cascade sur merchants/tenant_users/merchant_users/etc.
ALTER TABLE tenants ADD COLUMN archived_at TIMESTAMPTZ;
CREATE INDEX idx_tenants_archived_at ON tenants(archived_at);
