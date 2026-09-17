-- Selectable delivery options managed by admins.
CREATE TABLE IF NOT EXISTS shipping_methods (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    cost NUMERIC(10, 2) NOT NULL DEFAULT 0,
    estimated_days VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO shipping_methods (id, name, description, cost, estimated_days, active, display_order)
SELECT * FROM (VALUES
    ('a1000000-0000-4000-8000-000000000001', 'Standard delivery', 'Nationwide courier, tracked', 10.00, '2-3 business days', TRUE, 1),
    ('a1000000-0000-4000-8000-000000000002', 'Express delivery', 'Priority courier', 25.00, 'Next business day', TRUE, 2),
    ('a1000000-0000-4000-8000-000000000003', 'Store pickup', 'Collect from our Nairobi store', 0.00, 'Same day', TRUE, 3)
) AS seed(id, name, description, cost, estimated_days, active, display_order)
WHERE NOT EXISTS (SELECT 1 FROM shipping_methods);

-- Record which method an order used.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_method VARCHAR(100);
