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
(4, '手机', 1, 1, 1),
(5, '电脑', 1, 2, 1);

INSERT INTO `product` (`seller_id`, `name`, `description`, `price`, `stock`, `category_id`, `main_image`, `status`) VALUES
(1, 'iPhone 15', '苹果最新款手机', 5999.00, 100, 4, NULL, 1),
(1, 'MacBook Pro', 'M3芯片 16GB内存', 12999.00, 50, 5, NULL, 1),
(1, 'Java编程思想', '经典Java书籍', 99.00, 200, 2, NULL, 1),
(1, 'T恤 白色', '纯棉舒适', 79.00, 500, 3, NULL, 1);
