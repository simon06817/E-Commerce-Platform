CREATE TABLE `product_review` (
    `id`            bigint       NOT NULL AUTO_INCREMENT,
    `product_id`    bigint       NOT NULL,
    `buyer_id`      bigint       NOT NULL,
    `order_id`      bigint       NOT NULL,
    `order_item_id` bigint       NOT NULL,
    `rating`        tinyint      NOT NULL COMMENT '1-5',
    `content`       varchar(500) DEFAULT NULL,
    `create_time`   datetime     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_item` (`order_item_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_buyer_id` (`buyer_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
