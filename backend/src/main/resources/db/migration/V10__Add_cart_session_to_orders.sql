ALTER TABLE orders ADD COLUMN IF NOT EXISTS cart_session_id VARCHAR(255);
CREATE INDEX IF NOT EXISTS idx_orders_cart_session_id ON orders(cart_session_id);
