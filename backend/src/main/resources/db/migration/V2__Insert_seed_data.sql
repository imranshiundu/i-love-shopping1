-- V2__Insert_seed_data.sql
-- Seed data for development and testing

-- Insert brands
INSERT INTO brands (id, name, slug, logo, description, created_at, updated_at) VALUES
    ('a1000001-0000-4000-8000-000000000001', 'Lattice and Co.', 'lattice-co', NULL, 'Handcrafted ceramics and home goods with a modern aesthetic.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a1000001-0000-4000-8000-000000000002', 'Nordic Threads', 'nordic-threads', NULL, 'Sustainable Scandinavian textiles and accessories.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('a1000001-0000-4000-8000-000000000003', 'Urban Botanist', 'urban-botanist', NULL, 'Indoor plants and planters for city living.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert categories
INSERT INTO categories (id, name, slug, description, sort_order, created_at, updated_at) VALUES
    ('b1000001-0000-4000-8000-000000000001', 'Home and Living', 'home-living', 'Everything for your living space', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b1000001-0000-4000-8000-000000000002', 'Kitchen and Dining', 'kitchen-dining', 'Cookware, serveware, and dining essentials', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b1000001-0000-4000-8000-000000000003', 'Decor and Accessories', 'decor-accessories', 'Vases, candles, and decorative objects', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b1000001-0000-4000-8000-000000000004', 'Plants and Planters', 'plants-planters', 'Indoor plants and modern planters', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('b1000001-0000-4000-8000-000000000005', 'Textiles', 'textiles', 'Blankets, throws, and pillow covers', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Update child categories with parent_id
UPDATE categories SET parent_id = 'b1000001-0000-4000-8000-000000000001' WHERE slug IN ('kitchen-dining', 'decor-accessories', 'textiles');

-- Insert products
INSERT INTO products (id, name, slug, description, price, compare_at_price, sku, stock, weight, dimensions, is_active, category_id, brand_id, created_at, updated_at) VALUES
    ('c1000001-0000-4000-8000-000000000001', 'Speckled Ceramic Mug Set', 'speckled-ceramic-mug-set', 'Set of four hand-thrown stoneware mugs in a warm speckled glaze.', 48.00, 60.00, 'LTC-MUG-004', 42, 0.350, '{"length": 9, "width": 9, "height": 10, "unit": "cm"}'::jsonb, TRUE, 'b1000001-0000-4000-8000-000000000002', 'a1000001-0000-4000-8000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c1000001-0000-4000-8000-000000000002', 'Organic Cotton Throw Blanket', 'organic-cotton-throw-blanket', 'Woven from 100 percent GOTS-certified organic cotton.', 89.00, NULL, 'NT-THROW-012', 28, 0.850, '{"length": 130, "width": 170, "height": 2, "unit": "cm"}'::jsonb, TRUE, 'b1000001-0000-4000-8000-000000000005', 'a1000001-0000-4000-8000-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c1000001-0000-4000-8000-000000000003', 'Monstera Deliciosa in Terracotta Planter', 'monstera-deliciosa-terracotta-planter', 'Mature Monstera deliciosa in a hand-thrown terracotta planter.', 67.00, NULL, 'UB-MONS-003', 15, 3.200, '{"length": 25, "width": 25, "height": 60, "unit": "cm"}'::jsonb, TRUE, 'b1000001-0000-4000-8000-000000000004', 'a1000001-0000-4000-8000-000000000003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c1000001-0000-4000-8000-000000000004', 'Hand-Poured Soy Candle - Cedar and Moss', 'soy-candle-cedar-moss', 'Small-batch soy wax candle scented with cedarwood and oakmoss.', 32.00, 38.00, 'LTC-CNDL-007', 63, 0.450, '{"length": 8, "width": 8, "height": 10, "unit": "cm"}'::jsonb, TRUE, 'b1000001-0000-4000-8000-000000000003', 'a1000001-0000-4000-8000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c1000001-0000-4000-8000-000000000005', 'Linen Napkin Set - Natural', 'linen-napkin-set-natural', 'Set of six European linen napkins in undyed natural tone.', 54.00, NULL, 'NT-NAPK-006', 37, 0.280, '{"length": 45, "width": 45, "height": 0.5, "unit": "cm"}'::jsonb, TRUE, 'b1000001-0000-4000-8000-000000000002', 'a1000001-0000-4000-8000-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c1000001-0000-4000-8000-000000000006', 'Snake Plant Laurentii in Concrete Pot', 'snake-plant-laurentii-concrete-pot', 'Sansevieria trifasciata in a minimalist concrete planter.', 44.00, NULL, 'UB-SNAKE-001', 22, 2.100, '{"length": 18, "width": 18, "height": 45, "unit": "cm"}'::jsonb, TRUE, 'b1000001-0000-4000-8000-000000000004', 'a1000001-0000-4000-8000-000000000003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c1000001-0000-4000-8000-000000000007', 'Ceramic Serving Bowl - Large', 'ceramic-serving-bowl-large', 'Generous hand-thrown serving bowl in a matte cream glaze.', 58.00, NULL, 'LTC-BOWL-009', 19, 1.200, '{"length": 28, "width": 28, "height": 12, "unit": "cm"}'::jsonb, TRUE, 'b1000001-0000-4000-8000-000000000002', 'a1000001-0000-4000-8000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c1000001-0000-4000-8000-000000000008', 'Merino Wool Blanket - Charcoal', 'merino-wool-blanket-charcoal', 'Premium 100 percent merino wool blanket in deep charcoal.', 142.00, 165.00, 'NT-WOOL-015', 11, 1.800, '{"length": 140, "width": 200, "height": 0.8, "unit": "cm"}'::jsonb, TRUE, 'b1000001-0000-4000-8000-000000000005', 'a1000001-0000-4000-8000-000000000002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert product images
INSERT INTO product_images (id, product_id, url, alt, sort_order, created_at) VALUES
    ('d1000001-0000-4000-8000-000000000001', 'c1000001-0000-4000-8000-000000000001', 'https://picsum.photos/seed/mug1/800/600', 'Mug Set front', 0, CURRENT_TIMESTAMP),
    ('d1000001-0000-4000-8000-000000000002', 'c1000001-0000-4000-8000-000000000001', 'https://picsum.photos/seed/mug2/800/600', 'Mug Set detail', 1, CURRENT_TIMESTAMP),
    ('d1000001-0000-4000-8000-000000000003', 'c1000001-0000-4000-8000-000000000002', 'https://picsum.photos/seed/blanket1/800/600', 'Throw Blanket front', 0, CURRENT_TIMESTAMP),
    ('d1000001-0000-4000-8000-000000000004', 'c1000001-0000-4000-8000-000000000002', 'https://picsum.photos/seed/blanket2/800/600', 'Throw Blanket detail', 1, CURRENT_TIMESTAMP),
    ('d1000001-0000-4000-8000-000000000005', 'c1000001-0000-4000-8000-000000000003', 'https://picsum.photos/seed/monstera1/800/600', 'Monstera front', 0, CURRENT_TIMESTAMP),
    ('d1000001-0000-4000-8000-000000000006', 'c1000001-0000-4000-8000-000000000003', 'https://picsum.photos/seed/monstera2/800/600', 'Monstera detail', 1, CURRENT_TIMESTAMP),
    ('d1000001-0000-4000-8000-000000000007', 'c1000001-0000-4000-8000-000000000004', 'https://picsum.photos/seed/candle1/800/600', 'Soy Candle front', 0, CURRENT_TIMESTAMP),
    ('d1000001-0000-4000-8000-000000000008', 'c1000001-0000-4000-8000-000000000004', 'https://picsum.photos/seed/candle2/800/600', 'Soy Candle detail', 1, CURRENT_TIMESTAMP),
    ('d1000001-0000-4000-8000-000000000009', 'c1000001-0000-4000-8000-000000000005', 'https://picsum.photos/seed/napkin1/800/600', 'Napkin Set front', 0, CURRENT_TIMESTAMP),
    ('d1000001-0000-4000-8000-000000000010', 'c1000001-0000-4000-8000-000000000005', 'https://picsum.photos/seed/napkin2/800/600', 'Napkin Set detail', 1, CURRENT_TIMESTAMP),
    ('d1000001-0000-4000-8000-000000000011', 'c1000001-0000-4000-8000-000000000006', 'https://picsum.photos/seed/snake1/800/600', 'Snake Plant front', 0, CURRENT_TIMESTAMP),
    ('d1000001-0000-4000-8000-000000000012', 'c1000001-0000-4000-8000-000000000006', 'https://picsum.photos/seed/snake2/800/600', 'Snake Plant detail', 1, CURRENT_TIMESTAMP),
    ('d1000001-0000-4000-8000-000000000013', 'c1000001-0000-4000-8000-000000000007', 'https://picsum.photos/seed/bowl1/800/600', 'Serving Bowl front', 0, CURRENT_TIMESTAMP),
    ('d1000001-0000-4000-8000-000000000014', 'c1000001-0000-4000-8000-000000000007', 'https://picsum.photos/seed/bowl2/800/600', 'Serving Bowl detail', 1, CURRENT_TIMESTAMP),
    ('d1000001-0000-4000-8000-000000000015', 'c1000001-0000-4000-8000-000000000008', 'https://picsum.photos/seed/wool1/800/600', 'Wool Blanket front', 0, CURRENT_TIMESTAMP),
    ('d1000001-0000-4000-8000-000000000016', 'c1000001-0000-4000-8000-000000000008', 'https://picsum.photos/seed/wool2/800/600', 'Wool Blanket detail', 1, CURRENT_TIMESTAMP);
