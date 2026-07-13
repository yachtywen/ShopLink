CREATE DATABASE IF NOT EXISTS `dianping` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `dianping`;

SET @idx_count := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'tb_voucher_order'
    AND INDEX_NAME = 'uk_user_voucher'
);
SET @ddl := IF(
  @idx_count = 0,
  'ALTER TABLE `tb_voucher_order` ADD UNIQUE KEY `uk_user_voucher` (`user_id`, `voucher_id`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `tb_mq_message` (
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

CREATE TABLE IF NOT EXISTS `tb_dead_letter_message` (
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

INSERT INTO `tb_user` (`id`, `phone`, `nick_name`, `icon`)
VALUES (1, '13686869696', 'test_user', '/imgs/icons/default-icon.png')
ON DUPLICATE KEY UPDATE `nick_name` = VALUES(`nick_name`);

INSERT INTO `tb_user_info` (`user_id`, `city`, `introduce`)
VALUES (1, 'Hangzhou', 'local test user')
ON DUPLICATE KEY UPDATE `city` = VALUES(`city`);

INSERT INTO `tb_shop_type` (`id`, `name`, `icon`, `sort`)
VALUES (1, 'Food', '/types/ms.png', 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

INSERT INTO `tb_shop` (`id`, `name`, `type_id`, `images`, `area`, `address`, `x`, `y`, `avg_price`, `sold`, `comments`, `score`, `open_hours`)
VALUES (1, 'Dianping Test Shop', 1, '/imgs/shops/test-shop.jpg', 'Gongshu', 'No.1 Test Road', 120.149192, 30.316078, 80, 0, 0, 45, '10:00-22:00')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

INSERT INTO `tb_voucher` (`id`, `shop_id`, `title`, `sub_title`, `rules`, `pay_value`, `actual_value`, `type`, `status`)
VALUES (1, 1, '50 Yuan Seckill Voucher', 'Local test seckill voucher', 'Test only', 4750, 5000, 1, 1)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);

INSERT INTO `tb_seckill_voucher` (`voucher_id`, `stock`, `begin_time`, `end_time`)
VALUES (1, 100, '2026-01-01 00:00:00', '2036-01-01 00:00:00')
ON DUPLICATE KEY UPDATE `stock` = VALUES(`stock`);
