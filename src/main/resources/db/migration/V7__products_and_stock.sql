-- Catalogue produits par tenant/merchant
CREATE TABLE products (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    merchant_id     UUID NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    sku             VARCHAR(100),
    category        VARCHAR(100),
    unit_price      NUMERIC(12, 2) NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, sku)
);

-- Mouvements de stock (in/out/adjustment)
CREATE TABLE stock_movements (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    product_id  UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    type        VARCHAR(20) NOT NULL,           -- IN, OUT, ADJUSTMENT
    quantity    INT NOT NULL,
    note        TEXT,
    order_id    UUID REFERENCES orders(id) ON DELETE SET NULL,
    created_by  UUID REFERENCES tenant_users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Vue agrégée du stock courant par produit
CREATE VIEW product_stock AS
SELECT
    p.id           AS product_id,
    p.tenant_id,
    p.merchant_id,
    p.name,
    p.sku,
    COALESCE(SUM(
        CASE
            WHEN sm.type = 'IN'         THEN  sm.quantity
            WHEN sm.type = 'OUT'        THEN -sm.quantity
            WHEN sm.type = 'ADJUSTMENT' THEN  sm.quantity
            ELSE 0
        END
    ), 0) AS current_stock
FROM products p
LEFT JOIN stock_movements sm ON sm.product_id = p.id
GROUP BY p.id, p.tenant_id, p.merchant_id, p.name, p.sku;
