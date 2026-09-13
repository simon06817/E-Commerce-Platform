CREATE TABLE `order_notification` (
    `id`             bigint       NOT NULL AUTO_INCREMENT,
    `order_id`       bigint       NOT NULL,
    `event_type`     varchar(50)  NOT NULL,
    `recipient_id`   bigint       NOT NULL,
    `recipient_role` varchar(20)  NOT NULL,
    `title`          varchar(100) NOT NULL,
    `content`        varchar(500) NOT NULL,
    `is_read`        tinyint      DEFAULT 0,
    `create_time`    datetime     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_event_recipient` (`order_id`, `event_type`, `recipient_id`),
    KEY `idx_recipient` (`recipient_role`, `recipient_id`, `is_read`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
