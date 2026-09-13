USE `E-Commerce_Platform`;
SET NAMES utf8mb4;

-- BCrypt hash for password: 123456
SET @demo_password = '$2a$10$CybQusAxxBrkYBSVzIOCIeV4P6l5AelPOxJtNrf4FZW4u9RFvIHba';

-- Buyers 03-50 (existing buyer01/buyer02 keep their ids)
INSERT IGNORE INTO `user_buyer` (`username`, `password`, `nickname`, `phone`, `email`, `address`)
WITH RECURSIVE seq(n) AS (
    SELECT 3
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 50
)
SELECT
    CONCAT('buyer', LPAD(n, 2, '0')),
    @demo_password,
    CONCAT('buyer ', n),
    CONCAT('1380000', LPAD(n, 4, '0')),
    CONCAT('buyer', LPAD(n, 2, '0'), '@example.com'),
    'Demo City'
FROM seq;

-- Sellers 03-10 (existing seller01/seller02 keep their ids)
INSERT IGNORE INTO `user_seller` (`username`, `password`, `shop_name`, `phone`, `email`)
WITH RECURSIVE seq(n) AS (
    SELECT 3
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 10
)
SELECT
    CONCAT('seller', LPAD(n, 2, '0')),
    @demo_password,
    CONCAT('Shop ', LPAD(n, 2, '0')),
    CONCAT('1390000', LPAD(n, 4, '0')),
    CONCAT('seller', LPAD(n, 2, '0'), '@example.com')
FROM seq;

-- Add 96 products so that every seller has 10 products.
-- Existing 4 products: seller01 has 4, seller02 has 0.
-- Category distribution after insert: category 1-5 each has 20 products.
INSERT INTO `product` (`seller_id`, `name`, `description`, `price`, `stock`,
                       `category_id`, `main_image`, `status`)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 96
),
ranked AS (
    SELECT
        n,
        CASE
            WHEN n <= 6 THEN 1
            WHEN n <= 16 THEN 2
            ELSE 2 + CEIL((n - 16) / 10)
        END AS seller_id,
        CASE
            WHEN n <= 20 THEN 1
            WHEN n <= 39 THEN 2
            WHEN n <= 58 THEN 3
            WHEN n <= 77 THEN 4
            ELSE 5
        END AS category_id
    FROM seq
)
SELECT
    seller_id,
    CONCAT('Product-', seller_id, '-', LPAD(n, 2, '0')),
    CONCAT('Demo product for category ', category_id),
    ROUND(19.90 + n * 7.50, 2),
    10 + MOD(n, 90),
    category_id,
    NULL,
    1
FROM ranked
WHERE NOT EXISTS (
    SELECT 1 FROM `product` WHERE `name` LIKE 'Product-%'
);

-- Verification queries
SELECT 'buyer_count' AS metric, COUNT(*) AS value FROM `user_buyer`
UNION ALL
SELECT 'seller_count', COUNT(*) FROM `user_seller`
UNION ALL
SELECT 'product_count', COUNT(*) FROM `product`;

SELECT seller_id, COUNT(*) AS product_count
FROM `product`
GROUP BY seller_id
ORDER BY seller_id;

SELECT category_id, COUNT(*) AS product_count
FROM `product`
GROUP BY category_id
ORDER BY category_id;

SELECT id, username, password, phone, email, create_time
FROM `user_admin`;
