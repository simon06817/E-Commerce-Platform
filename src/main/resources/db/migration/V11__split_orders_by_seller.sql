ALTER TABLE `order_info`
    ADD COLUMN `seller_id` bigint DEFAULT NULL COMMENT 'user_seller.id for newly split orders' AFTER `buyer_id`,
    ADD COLUMN `checkout_group_id` varchar(64) DEFAULT NULL COMMENT 'one buyer checkout submission' AFTER `idempotency_key`;

CREATE INDEX `idx_order_seller_id` ON `order_info` (`seller_id`);
CREATE INDEX `idx_buyer_checkout_group` ON `order_info` (`buyer_id`, `checkout_group_id`);
