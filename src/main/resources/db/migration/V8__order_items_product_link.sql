-- Lien optionnel entre une ligne de commande et un produit du catalogue,
-- pour permettre la déduction/restauration automatique du stock.
ALTER TABLE order_items ADD COLUMN product_id UUID REFERENCES products(id) ON DELETE SET NULL;
CREATE INDEX idx_order_items_product ON order_items(product_id);
