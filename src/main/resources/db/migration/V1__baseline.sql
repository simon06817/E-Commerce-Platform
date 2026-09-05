CREATE TABLE `user_admin` (
    `id`          bigint       NOT NULL AUTO_INCREMENT,
    `username`    varchar(50)  NOT NULL,
    `password`    varchar(100) NOT NULL,
    `phone`       varchar(20)  DEFAULT NULL,
    `email`       varchar(100) DEFAULT NULL,
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     tinyint      DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `user_buyer` (
    `id`          bigint       NOT NULL AUTO_INCREMENT,
    `username`    varchar(50)  NOT NULL,
    `password`    varchar(100) NOT NULL,
    `nickname`    varchar(50)  DEFAULT NULL,
    `phone`       varchar(20)  DEFAULT NULL,
    `email`       varchar(100) DEFAULT NULL,
    `address`     varchar(255) DEFAULT NULL,
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     tinyint      DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `user_seller` (
    `id`          bigint       NOT NULL AUTO_INCREMENT,
    `username`    varchar(50)  NOT NULL,
    `password`    varchar(100) NOT NULL,
    `shop_name`   varchar(100) DEFAULT NULL,
    `phone`       varchar(20)  DEFAULT NULL,
    `email`       varchar(100) DEFAULT NULL,
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     tinyint      DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `product_category` (
    `id`          bigint      NOT NULL AUTO_INCREMENT,
    `name`        varchar(50) NOT NULL,
    `parent_id`   bigint      DEFAULT 0,
    `sort_order`  int         DEFAULT 0,
    `create_time` datetime    DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `status`      int         DEFAULT 1,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `product` (
    `id`          bigint         NOT NULL AUTO_INCREMENT,
    `seller_id`   bigint         NOT NULL,
    `name`        varchar(100)   NOT NULL,
    `description` text,
    `price`       decimal(10, 2) NOT NULL,
    `stock`       int            DEFAULT 0,
    `category_id` bigint         DEFAULT NULL,
    `main_image`  varchar(255)   DEFAULT NULL,
    `status`      tinyint        DEFAULT 1,
    `create_time` datetime       DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_seller_id` (`seller_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `cart` (
    `id`          bigint     NOT NULL AUTO_INCREMENT,
    `buyer_id`    bigint     NOT NULL,
    `product_id`  bigint     NOT NULL,
    `quantity`    int        NOT NULL DEFAULT 1,
    `checked`     tinyint(1) DEFAULT 1,
    `create_time` datetime   DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_buyer_product` (`buyer_id`, `product_id`),
    KEY `idx_buyer_id` (`buyer_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `order_info` (
    `id`               bigint         NOT NULL AUTO_INCREMENT,
    `order_no`         varchar(32)    NOT NULL,
    `idempotency_key`  varchar(64)    DEFAULT NULL,
    `buyer_id`         bigint         NOT NULL,
    `total_amount`     decimal(10, 2) NOT NULL,
    `status`           tinyint        DEFAULT 0,
    `receiver_name`    varchar(50)    DEFAULT NULL,
    `receiver_phone`   varchar(20)    DEFAULT NULL,
    `receiver_address` varchar(255)   DEFAULT NULL,
    `create_time`      datetime       DEFAULT CURRENT_TIMESTAMP,
    `update_time`      datetime       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `payment_time`     datetime       DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    UNIQUE KEY `uk_idempotency_key` (`idempotency_key`),
    KEY `idx_buyer_id` (`buyer_id`),
    KEY `idx_status_create_time` (`status`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `order_item` (
    `id`            bigint         NOT NULL AUTO_INCREMENT,
    `order_id`      bigint         NOT NULL,
    `product_id`    bigint         NOT NULL,
    `product_name`  varchar(100)   NOT NULL,
    `product_image` varchar(255)   DEFAULT NULL,
    `price`         decimal(10, 2) NOT NULL,
    `quantity`      int            NOT NULL,
    `subtotal`      decimal(10, 2) NOT NULL,
    `create_time`   datetime       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `order_outbox` (
    `id`          bigint       NOT NULL AUTO_INCREMENT,
    `order_id`    bigint       NOT NULL,
    `event_type`  varchar(50)  NOT NULL,
    `payload`     varchar(2000) DEFAULT NULL,
    `status`      tinyint      DEFAULT 0,
    `retry_count` int          DEFAULT 0,
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status_create_time` (`status`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `order_event_record` (
    `id`          bigint       NOT NULL AUTO_INCREMENT,
    `order_id`    bigint       NOT NULL,
    `event_type`  varchar(50)  NOT NULL,
    `payload`     varchar(2000) DEFAULT NULL,
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_event` (`order_id`, `event_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
