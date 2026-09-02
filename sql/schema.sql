-- E-Commerce-Platform database schema
CREATE DATABASE IF NOT EXISTS `E-Commerce_Platform`
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `E-Commerce_Platform`;

DROP TABLE IF EXISTS `order_item`;
DROP TABLE IF EXISTS `order_info`;
DROP TABLE IF EXISTS `cart`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `product_category`;
DROP TABLE IF EXISTS `user_seller`;
DROP TABLE IF EXISTS `user_buyer`;
DROP TABLE IF EXISTS `user_admin`;

CREATE TABLE `user_admin` (
    `id`          bigint       NOT NULL AUTO_INCREMENT,
    `username`    varchar(50)  NOT NULL COMMENT 'login name',
    `password`    varchar(100) NOT NULL COMMENT 'BCrypt hash',
    `phone`       varchar(20)  DEFAULT NULL,
    `email`       varchar(100) DEFAULT NULL,
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     tinyint      DEFAULT 0 COMMENT '0 active, 1 deleted',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `user_buyer` (
    `id`          bigint       NOT NULL AUTO_INCREMENT,
    `username`    varchar(50)  NOT NULL COMMENT 'login name',
    `password`    varchar(100) NOT NULL COMMENT 'BCrypt hash',
    `nickname`    varchar(50)  DEFAULT NULL,
    `phone`       varchar(20)  DEFAULT NULL,
    `email`       varchar(100) DEFAULT NULL,
    `address`     varchar(255) DEFAULT NULL,
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     tinyint      DEFAULT 0 COMMENT '0 active, 1 deleted',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `user_seller` (
    `id`          bigint       NOT NULL AUTO_INCREMENT,
    `username`    varchar(50)  NOT NULL COMMENT 'login name',
    `password`    varchar(100) NOT NULL COMMENT 'BCrypt hash',
    `shop_name`   varchar(100) DEFAULT NULL,
    `phone`       varchar(20)  DEFAULT NULL,
    `email`       varchar(100) DEFAULT NULL,
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     tinyint      DEFAULT 0 COMMENT '0 active, 1 deleted',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `product_category` (
    `id`          bigint      NOT NULL AUTO_INCREMENT,
    `name`        varchar(50) NOT NULL,
    `parent_id`   bigint      DEFAULT 0 COMMENT '0 means root',
    `sort_order`  int         DEFAULT 0,
    `create_time` datetime    DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `status`      int         DEFAULT 1 COMMENT '0 disabled, 1 enabled',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `product` (
    `id`          bigint         NOT NULL AUTO_INCREMENT,
    `seller_id`   bigint         NOT NULL COMMENT 'owner seller, user_seller.id',
    `name`        varchar(100)   NOT NULL,
    `description` text,
    `price`       decimal(10, 2) NOT NULL,
    `stock`       int            DEFAULT 0,
    `category_id` bigint         DEFAULT NULL,
    `main_image`  varchar(255)   DEFAULT NULL,
    `status`      tinyint        DEFAULT 1 COMMENT '1 on sale, 0 off sale',
    `create_time` datetime       DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_seller_id` (`seller_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `cart` (
    `id`          bigint     NOT NULL AUTO_INCREMENT,
    `buyer_id`    bigint     NOT NULL COMMENT 'user_buyer.id',
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
    `buyer_id`         bigint         NOT NULL COMMENT 'user_buyer.id',
    `total_amount`     decimal(10, 2) NOT NULL,
    `status`           tinyint        DEFAULT 0 COMMENT '0 unpaid, 1 paid, 2 shipped, 3 completed, 4 canceled',
    `receiver_name`    varchar(50)    DEFAULT NULL,
    `receiver_phone`   varchar(20)    DEFAULT NULL,
    `receiver_address` varchar(255)   DEFAULT NULL,
    `create_time`      datetime       DEFAULT CURRENT_TIMESTAMP,
    `update_time`      datetime       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `payment_time`     datetime       DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_buyer_id` (`buyer_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `order_item` (
    `id`            bigint         NOT NULL AUTO_INCREMENT,
    `order_id`      bigint         NOT NULL COMMENT 'order_info.id',
    `product_id`    bigint         NOT NULL,
    `product_name`  varchar(100)   NOT NULL COMMENT 'name snapshot',
    `product_image` varchar(255)   DEFAULT NULL,
    `price`         decimal(10, 2) NOT NULL COMMENT 'unit price snapshot',
    `quantity`      int            NOT NULL,
    `subtotal`      decimal(10, 2) NOT NULL,
    `create_time`   datetime       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
