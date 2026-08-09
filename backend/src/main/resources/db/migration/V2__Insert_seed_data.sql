-- V2__Insert_seed_data.sql
-- Seed data for development and testing

-- Insert brands
INSERT INTO brands (id, name, slug, logo, description, created_at, updated_at) VALUES
    (uuid_generate_v4(), 'Lattice & Co.', 'lattice-co', null, 'Handcrafted ceramics and home goods with a modern aesthetic.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), 'Nordic Threads', 'nordic-threads', null, 'Sustainable Scandinavian textiles and accessories.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), 'Urban Botanist', 'urban-botanist', null, 'Indoor plants and planters for city living.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert categories
WITH brand_lattice AS (SELECT id FROM brands WHERE slug = 'lattice-co'),
     brand_nordic AS (SELECT id FROM brands WHERE slug = 'nordic-threads'),
     brand_urban AS (SELECT id FROM brands WHERE slug = 'urban-botanist'),
     cat_home AS (
         INSERT INTO categories (id, name, slug, description, sort_order, created_at, updated_at)
         VALUES (uuid_generate_v4(), 'Home & Living', 'home-living', 'Everything for your living space', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
         RETURNING id
     ),
     cat_kitchen AS (
         INSERT INTO categories (id, name, slug, description, sort_order, parent_id, created_at, updated_at)
         VALUES (uuid_generate_v4(), 'Kitchen & Dining', 'kitchen-dining', 'Cookware, serveware, and dining essentials', 2, (SELECT id FROM cat_home), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
         RETURNING id
     ),
     cat_decor AS (
         INSERT INTO categories (id, name, slug, description, sort_order, parent_id, created_at, updated_at)
         VALUES (uuid_generate_v4(), 'Decor & Accessories', 'decor-accessories', 'Vases, candles, and decorative objects', 3, (SELECT id FROM cat_home), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
         RETURNING id
     ),
     cat_plants AS (
         INSERT INTO categories (id, name, slug, description, sort_order, created_at, updated_at)
         VALUES (uuid_generate_v4(), 'Plants & Planters', 'plants-planters', 'Indoor plants and modern planters', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
         RETURNING id
     ),
     cat_textiles AS (
         INSERT INTO categories (id, name, slug, description, sort_order, parent_id, created_at, updated_at)
         VALUES (uuid_generate_v4(), 'Textiles', 'textiles', 'Blankets, throws, and pillow covers', 5, (SELECT id FROM cat_home), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
         RETURNING id
     )
INSERT INTO products (id, name, slug, description, price, compare_at_price, sku, stock, weight, dimensions, is_active, category_id, brand_id, created_at, updated_at)
SELECT uuid_generate_v4(), 'Speckled Ceramic Mug Set', 'speckled-ceramic-mug-set',
    'Set of four hand-thrown stoneware mugs in a warm speckled glaze. Each piece is unique with subtle variations in color and texture. Perfect for morning coffee or afternoon tea.',
    48.00, 60.00, 'LTC-MUG-004', 42, 0.350, '{"length": 9, "width": 9, "height": 10, "unit": "cm"}'::jsonb, TRUE,
    c.id, b.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM cat_kitchen c, brand_lattice b
UNION ALL
SELECT uuid_generate_v4(), 'Organic Cotton Throw Blanket', 'organic-cotton-throw-blanket',
    'Woven from 100% GOTS-certified organic cotton. This medium-weight throw features a subtle herringbone pattern and hand-finished fringed edges. Naturally breathable and gets softer with every wash.',
    89.00, NULL, 'NT-THROW-012', 28, 0.850, '{"length": 130, "width": 170, "height": 2, "unit": "cm"}'::jsonb, TRUE,
    c.id, b.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM cat_textiles c, brand_nordic b
UNION ALL
SELECT uuid_generate_v4(), 'Monstera Deliciosa in Terracotta Planter', 'monstera-deliciosa-terracotta-planter',
    'Mature Monstera deliciosa (Swiss cheese plant) in a hand-thrown terracotta planter with drainage hole and matching saucer. Plant height approximately 60cm. Comes with care guide.',
    67.00, NULL, 'UB-MONS-003', 15, 3.200, '{"length": 25, "width": 25, "height": 60, "unit": "cm"}'::jsonb, TRUE,
    c.id, b.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM cat_plants c, brand_urban b
UNION ALL
SELECT uuid_generate_v4(), 'Hand-Poured Soy Candle - Cedar & Moss', 'soy-candle-cedar-moss',
    'Small-batch soy wax candle scented with cedarwood, oakmoss, and a hint of bergamot. 50-hour burn time. Hand-poured in a reusable amber glass jar with a wooden lid.',
    32.00, 38.00, 'LTC-CNDL-007', 63, 0.450, '{"length": 8, "width": 8, "height": 10, "unit": "cm"}'::jsonb, TRUE,
    c.id, b.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM cat_decor c, brand_lattice b
UNION ALL
SELECT uuid_generate_v4(), 'Linen Napkin Set - Natural', 'linen-napkin-set-natural',
    'Set of six European linen napkins in undyed natural tone. Stone-washed for exceptional softness. Oversized at 45x45cm for versatile styling. Machine washable, gets better with age.',
    54.00, NULL, 'NT-NAPK-006', 37, 0.280, '{"length": 45, "width": 45, "height": 0.5, "unit": "cm"}'::jsonb, TRUE,
    c.id, b.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM cat_kitchen c, brand_nordic b
UNION ALL
SELECT uuid_generate_v4(), 'Snake Plant Laurentii in Concrete Pot', 'snake-plant-laurentii-concrete-pot',
    'Sansevieria trifasciata "Laurentii" in a minimalist concrete planter. Extremely low-maintenance, tolerates low light. Height ~45cm. Includes care card and decorative gravel topper.',
    44.00, NULL, 'UB-SNAKE-001', 22, 2.100, '{"length": 18, "width": 18, "height": 45, "unit": "cm"}'::jsonb, TRUE,
    c.id, b.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM cat_plants c, brand_urban b
UNION ALL
SELECT uuid_generate_v4(), 'Ceramic Serving Bowl - Large', 'ceramic-serving-bowl-large',
    'Generous hand-thrown serving bowl in a matte cream glaze. Ideal for salads, pasta, or fruit display. Each piece bears the maker\'s mark. Dishwasher and microwave safe.',
    58.00, NULL, 'LTC-BOWL-009', 19, 1.200, '{"length": 28, "width": 28, "height": 12, "unit": "cm"}'::jsonb, TRUE,
    c.id, b.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM cat_kitchen c, brand_lattice b
UNION ALL
SELECT uuid_generate_v4(), 'Merino Wool Blanket - Charcoal', 'merino-wool-blanket-charcoal',
    'Premium 100% merino wool blanket in deep charcoal. Thermoregulating, naturally odor-resistant, and incredibly soft. Finished with a subtle whipstitch edge. Dry clean recommended.',
    142.00, 165.00, 'NT-WOOL-015', 11, 1.800, '{"length": 140, "width": 200, "height": 0.8, "unit": "cm"}'::jsonb, TRUE,
    c.id, b.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM cat_textiles c, brand_nordic b;

-- Insert product images
INSERT INTO product_images (id, product_id, url, alt, sort_order, created_at)
SELECT uuid_generate_v4(), p.id, 'https://picsum.photos/seed/' || p.slug || '-1/800/600', p.name, 0, CURRENT_TIMESTAMP
FROM products p
UNION ALL
SELECT uuid_generate_v4(), p.id, 'https://picsum.photos/seed/' || p.slug || '-2/800/600', p.name || ' detail', 1, CURRENT_TIMESTAMP
FROM products p;