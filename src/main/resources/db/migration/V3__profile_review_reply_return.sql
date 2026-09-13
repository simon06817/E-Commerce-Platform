ALTER TABLE `order_info`
    ADD COLUMN `complete_time` datetime DEFAULT NULL AFTER `payment_time`;

ALTER TABLE `product_review`
    ADD COLUMN `reply_content` varchar(500) DEFAULT NULL AFTER `content`,
    ADD COLUMN `reply_time` datetime DEFAULT NULL AFTER `reply_content`,
    ADD COLUMN `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `create_time`;

CREATE TABLE `order_return` (
    `id`            bigint         NOT NULL AUTO_INCREMENT,
    `order_id`      bigint         NOT NULL,
    `order_item_id` bigint         NOT NULL,
    `buyer_id`      bigint         NOT NULL,
    `seller_id`     bigint         NOT NULL,
    `product_id`    bigint         NOT NULL,
    `quantity`      int            NOT NULL,
    `reason`        varchar(500)   NOT NULL,
    `status`        tinyint        DEFAULT 0 COMMENT '0 applied, 1 approved/refunded, 2 rejected, 3 canceled',
    `refund_amount` decimal(10, 2) NOT NULL,
    `handle_note`   varchar(500)   DEFAULT NULL,
    `apply_time`    datetime       DEFAULT CURRENT_TIMESTAMP,
    `handle_time`   datetime       DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_item` (`order_item_id`),
    KEY `idx_buyer_id` (`buyer_id`),
    KEY `idx_seller_id` (`seller_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
