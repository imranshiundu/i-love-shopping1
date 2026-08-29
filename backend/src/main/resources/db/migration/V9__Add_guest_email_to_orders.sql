ALTER TABLE orders ADD COLUMN IF NOT EXISTS guest_email VARCHAR(255);
CREATE INDEX IF NOT EXISTS idx_orders_guest_email ON orders(guest_email);
