USE `E-Commerce_Platform`;
SET NAMES utf8mb4;

-- Passwords are BCrypt hashes (admin -> admin, other accounts -> 123456)
INSERT INTO `user_admin` (`username`, `password`, `phone`, `email`) VALUES
('admin', '$2a$10$IsJgO03jfZ2PfId5FNJxCO16YfIX3H.hM4bLOQ2yZdaBdv1AOX9oq', '13800000000', 'admin@example.com');

INSERT INTO `user_buyer` (`username`, `password`, `nickname`, `phone`, `email`, `address`) VALUES
('buyer01', '$2a$10$CybQusAxxBrkYBSVzIOCIeV4P6l5AelPOxJtNrf4FZW4u9RFvIHba', 'buyer one', '13800000001', 'buyer01@example.com', 'Beijing'),
('buyer02', '$2a$10$CybQusAxxBrkYBSVzIOCIeV4P6l5AelPOxJtNrf4FZW4u9RFvIHba', 'buyer two', '13800000002', 'buyer02@example.com', 'Shanghai');

INSERT INTO `user_seller` (`username`, `password`, `shop_name`, `phone`, `email`) VALUES
('seller01', '$2a$10$CybQusAxxBrkYBSVzIOCIeV4P6l5AelPOxJtNrf4FZW4u9RFvIHba', 'Tech Store', '13900000001', 'seller01@example.com'),
('seller02', '$2a$10$CybQusAxxBrkYBSVzIOCIeV4P6l5AelPOxJtNrf4FZW4u9RFvIHba', 'Book House', '13900000002', 'seller02@example.com');

INSERT INTO `product_category` (`id`, `name`, `parent_id`, `sort_order`, `status`) VALUES
(1, '电子产品', 0, 1, 1),
(2, '图书', 0, 2, 1),
(3, '服装', 0, 3, 1),
(4, '玩具', 0, 4, 1),
(5, '食品', 0, 5, 1);

INSERT INTO `product` (`seller_id`, `name`, `description`, `price`, `stock`, `category_id`, `main_image`, `status`) VALUES
(1, 'iPhone 15', 'iPhone 15，做工扎实，性能稳定，适合办公、学习、通勤与日常娱乐使用。', 5999.00, 100, 1, NULL, 1),
(1, 'MacBook Pro', 'MacBook Pro，做工扎实，性能稳定，适合办公、学习、通勤与日常娱乐使用。', 12999.00, 50, 1, NULL, 1),
(1, 'Java编程思想', 'Java编程思想，正版精选图书，内容系统全面，适合课程学习、技术提升与日常阅读。', 108.00, 200, 2, NULL, 1),
(1, 'T恤 白色', 'T恤 白色，选用舒适面料，版型合身，适合日常通勤、休闲与四季穿搭。', 79.00, 500, 3, NULL, 1);
