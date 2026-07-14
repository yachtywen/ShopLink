USE `dianping`;

DELIMITER //

DROP PROCEDURE IF EXISTS seed_dianping_test_data//
CREATE PROCEDURE seed_dianping_test_data()
BEGIN
  DECLARE i INT DEFAULT 2;
  DECLARE blog_i INT DEFAULT 1;
  DECLARE user_id BIGINT;
  DECLARE shop_id BIGINT;
  DECLARE type_id BIGINT;

  WHILE i <= 101 DO
    SET user_id = i;
    SET shop_id = i;
    SET type_id = i;

    INSERT INTO `tb_user` (`id`, `phone`, `password`, `nick_name`, `icon`)
    VALUES (
      user_id,
      CONCAT('1390000', LPAD(i, 4, '0')),
      '',
      CONCAT('seed_user_', i),
      '/imgs/icons/default-icon.png'
    )
    ON DUPLICATE KEY UPDATE
      `nick_name` = VALUES(`nick_name`),
      `icon` = VALUES(`icon`);

    INSERT INTO `tb_user_info` (`user_id`, `city`, `introduce`, `fans`, `followee`, `gender`, `credits`, `level`)
    VALUES (
      user_id,
      'Hangzhou',
      CONCAT('seed user profile ', i),
      i % 50,
      i % 30,
      i % 2,
      100 + i,
      i % 8
    )
    ON DUPLICATE KEY UPDATE
      `city` = VALUES(`city`),
      `introduce` = VALUES(`introduce`),
      `fans` = VALUES(`fans`),
      `followee` = VALUES(`followee`);

    INSERT INTO `tb_shop_type` (`id`, `name`, `icon`, `sort`)
    VALUES (
      type_id,
      CONCAT('Seed Type ', i),
      '/types/ms.png',
      i
    )
    ON DUPLICATE KEY UPDATE
      `name` = VALUES(`name`),
      `sort` = VALUES(`sort`);

    INSERT INTO `tb_shop` (`id`, `name`, `type_id`, `images`, `area`, `address`, `x`, `y`, `avg_price`, `sold`, `comments`, `score`, `open_hours`)
    VALUES (
      shop_id,
      CONCAT('Seed Shop ', i),
      type_id,
      '/imgs/shops/test-shop.jpg',
      'Gongshu',
      CONCAT('No.', i, ' Seed Road'),
      120.100000 + (i * 0.0005),
      30.280000 + (i * 0.0003),
      30 + (i % 200),
      i * 3,
      i,
      35 + (i % 15),
      '10:00-22:00'
    )
    ON DUPLICATE KEY UPDATE
      `name` = VALUES(`name`),
      `type_id` = VALUES(`type_id`),
      `x` = VALUES(`x`),
      `y` = VALUES(`y`),
      `sold` = VALUES(`sold`),
      `comments` = VALUES(`comments`);

    INSERT INTO `tb_voucher` (`id`, `shop_id`, `title`, `sub_title`, `rules`, `pay_value`, `actual_value`, `type`, `status`)
    VALUES (
      i,
      shop_id,
      CONCAT('Seed Seckill Voucher ', i),
      'Seed voucher for local testing',
      'Local seed data only',
      4750,
      5000,
      1,
      1
    )
    ON DUPLICATE KEY UPDATE
      `shop_id` = VALUES(`shop_id`),
      `title` = VALUES(`title`),
      `status` = VALUES(`status`);

    INSERT INTO `tb_seckill_voucher` (`voucher_id`, `stock`, `begin_time`, `end_time`)
    VALUES (
      i,
      100,
      '2026-01-01 00:00:00',
      '2036-01-01 00:00:00'
    )
    ON DUPLICATE KEY UPDATE
      `stock` = VALUES(`stock`),
      `begin_time` = VALUES(`begin_time`),
      `end_time` = VALUES(`end_time`);

    SET i = i + 1;
  END WHILE;

  WHILE blog_i <= 100 DO
    SET user_id = ((blog_i - 1) % 100) + 2;
    SET shop_id = ((blog_i - 1) % 100) + 2;

    INSERT INTO `tb_blog` (`id`, `shop_id`, `user_id`, `title`, `images`, `content`, `liked`, `comments`)
    VALUES (
      blog_i,
      shop_id,
      user_id,
      CONCAT('Seed Blog ', blog_i),
      '/imgs/blogs/test-blog.jpg',
      CONCAT('This is seed blog content ', blog_i),
      blog_i * 2,
      blog_i % 20
    )
    ON DUPLICATE KEY UPDATE
      `title` = VALUES(`title`),
      `liked` = VALUES(`liked`),
      `comments` = VALUES(`comments`);

    INSERT INTO `tb_blog_comments` (`id`, `user_id`, `blog_id`, `parent_id`, `answer_id`, `content`, `liked`, `status`)
    VALUES (
      blog_i,
      CASE WHEN user_id < 101 THEN user_id + 1 ELSE 2 END,
      blog_i,
      0,
      0,
      CONCAT('Seed comment ', blog_i),
      blog_i % 10,
      0
    )
    ON DUPLICATE KEY UPDATE
      `content` = VALUES(`content`),
      `liked` = VALUES(`liked`);

    INSERT INTO `tb_follow` (`id`, `user_id`, `follow_user_id`)
    VALUES (
      blog_i,
      user_id,
      CASE WHEN user_id < 101 THEN user_id + 1 ELSE 2 END
    )
    ON DUPLICATE KEY UPDATE
      `follow_user_id` = VALUES(`follow_user_id`);

    SET blog_i = blog_i + 1;
  END WHILE;
END//

CALL seed_dianping_test_data()//
DROP PROCEDURE IF EXISTS seed_dianping_test_data//

DELIMITER ;
