CREATE DATABASE IF NOT EXISTS `dianping` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `dianping`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `tb_blog`;
CREATE TABLE `tb_blog` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `shop_id` bigint(20) NOT NULL,
  `user_id` bigint(20) UNSIGNED NOT NULL,
  `title` varchar(255) NOT NULL,
  `images` varchar(2048) NOT NULL,
  `content` varchar(2048) NOT NULL,
  `liked` int(8) UNSIGNED DEFAULT 0,
  `comments` int(8) UNSIGNED DEFAULT 0,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `tb_blog_comments`;
CREATE TABLE `tb_blog_comments` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) UNSIGNED NOT NULL,
  `blog_id` bigint(20) UNSIGNED NOT NULL,
  `parent_id` bigint(20) UNSIGNED NOT NULL DEFAULT 0,
  `answer_id` bigint(20) UNSIGNED NOT NULL DEFAULT 0,
  `content` varchar(255) NOT NULL,
  `liked` int(8) UNSIGNED DEFAULT 0,
  `status` tinyint(1) UNSIGNED DEFAULT 0,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `tb_follow`;
CREATE TABLE `tb_follow` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) UNSIGNED NOT NULL,
  `follow_user_id` bigint(20) UNSIGNED NOT NULL,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_follow` (`user_id`, `follow_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `tb_seckill_voucher`;
CREATE TABLE `tb_seckill_voucher` (
  `voucher_id` bigint(20) UNSIGNED NOT NULL,
  `stock` int(8) NOT NULL,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `begin_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `end_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`voucher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `tb_shop`;
CREATE TABLE `tb_shop` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  `type_id` bigint(20) UNSIGNED NOT NULL,
  `images` varchar(1024) NOT NULL DEFAULT '',
  `area` varchar(128) DEFAULT NULL,
  `address` varchar(255) NOT NULL,
  `x` double UNSIGNED NOT NULL,
  `y` double UNSIGNED NOT NULL,
  `avg_price` bigint(10) UNSIGNED DEFAULT NULL,
  `sold` int(10) UNSIGNED NOT NULL DEFAULT 0,
  `comments` int(10) UNSIGNED NOT NULL DEFAULT 0,
  `score` int(2) UNSIGNED NOT NULL DEFAULT 0,
  `open_hours` varchar(32) DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_type_id` (`type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `tb_shop_type`;
CREATE TABLE `tb_shop_type` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` varchar(32) DEFAULT NULL,
  `icon` varchar(255) DEFAULT NULL,
  `sort` int(3) UNSIGNED DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `tb_user`;
CREATE TABLE `tb_user` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `phone` varchar(11) NOT NULL,
  `password` varchar(128) DEFAULT '',
  `nick_name` varchar(32) DEFAULT '',
  `icon` varchar(255) DEFAULT '',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `tb_user_info`;
CREATE TABLE `tb_user_info` (
  `user_id` bigint(20) UNSIGNED NOT NULL,
  `city` varchar(64) DEFAULT '',
  `introduce` varchar(128) DEFAULT NULL,
  `fans` int(8) UNSIGNED DEFAULT 0,
  `followee` int(8) UNSIGNED DEFAULT 0,
  `gender` tinyint(1) UNSIGNED DEFAULT 0,
  `birthday` date DEFAULT NULL,
  `credits` int(8) UNSIGNED DEFAULT 0,
  `level` tinyint(1) UNSIGNED DEFAULT 0,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `tb_voucher`;
CREATE TABLE `tb_voucher` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `shop_id` bigint(20) UNSIGNED DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `sub_title` varchar(255) DEFAULT NULL,
  `rules` varchar(1024) DEFAULT NULL,
  `pay_value` bigint(10) UNSIGNED NOT NULL,
  `actual_value` bigint(10) NOT NULL,
  `type` tinyint(1) UNSIGNED NOT NULL DEFAULT 0,
  `status` tinyint(1) UNSIGNED NOT NULL DEFAULT 1,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `tb_voucher_order`;
CREATE TABLE `tb_voucher_order` (
  `id` bigint(20) NOT NULL,
  `user_id` bigint(20) UNSIGNED NOT NULL,
  `voucher_id` bigint(20) UNSIGNED NOT NULL,
  `pay_type` tinyint(1) UNSIGNED NOT NULL DEFAULT 1,
  `status` tinyint(1) UNSIGNED NOT NULL DEFAULT 1,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `pay_time` timestamp NULL DEFAULT NULL,
  `use_time` timestamp NULL DEFAULT NULL,
  `refund_time` timestamp NULL DEFAULT NULL,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_voucher` (`user_id`, `voucher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `tb_mq_message`;
CREATE TABLE `tb_mq_message` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `message_id` varchar(128) DEFAULT NULL,
  `order_id` bigint(20) NOT NULL,
  `user_id` bigint(20) UNSIGNED NOT NULL,
  `voucher_id` bigint(20) UNSIGNED NOT NULL,
  `topic` varchar(128) NOT NULL,
  `message_body` varchar(2048) NOT NULL,
  `status` tinyint(1) UNSIGNED NOT NULL DEFAULT 0,
  `retry_count` int(8) UNSIGNED NOT NULL DEFAULT 0,
  `max_retry_count` int(8) UNSIGNED NOT NULL DEFAULT 5,
  `next_retry_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_error` varchar(1024) DEFAULT NULL,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_topic` (`order_id`, `topic`),
  KEY `idx_status_retry_time` (`status`, `next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `tb_dead_letter_message`;
CREATE TABLE `tb_dead_letter_message` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `topic` varchar(128) NOT NULL,
  `consumer_group` varchar(128) NOT NULL,
  `message_body` varchar(4096) DEFAULT NULL,
  `error_message` varchar(1024) DEFAULT NULL,
  `reconsume_times` int(8) UNSIGNED DEFAULT 0,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_topic_group` (`topic`, `consumer_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `tb_user` (`id`, `phone`, `nick_name`, `icon`) VALUES
  (1, '13686869696', 'test_user', '/imgs/icons/default-icon.png');

INSERT INTO `tb_user_info` (`user_id`, `city`, `introduce`) VALUES
  (1, 'Hangzhou', 'local test user');

INSERT INTO `tb_shop_type` (`id`, `name`, `icon`, `sort`) VALUES
  (1, 'Food', '/types/ms.png', 1);

INSERT INTO `tb_shop` (`id`, `name`, `type_id`, `images`, `area`, `address`, `x`, `y`, `avg_price`, `sold`, `comments`, `score`, `open_hours`) VALUES
  (1, 'Dianping Test Shop', 1, '/imgs/shops/test-shop.jpg', 'Gongshu', 'No.1 Test Road', 120.149192, 30.316078, 80, 0, 0, 45, '10:00-22:00');

INSERT INTO `tb_voucher` (`id`, `shop_id`, `title`, `sub_title`, `rules`, `pay_value`, `actual_value`, `type`, `status`) VALUES
  (1, 1, '50 Yuan Seckill Voucher', 'Local test seckill voucher', 'Test only', 4750, 5000, 1, 1);

INSERT INTO `tb_seckill_voucher` (`voucher_id`, `stock`, `begin_time`, `end_time`) VALUES
  (1, 100, '2026-01-01 00:00:00', '2036-01-01 00:00:00');

SET FOREIGN_KEY_CHECKS = 1;
