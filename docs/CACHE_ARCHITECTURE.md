# 商户与优惠券多级缓存架构

## 范围

本实现仅缓存 `GET /shop/{id}` 的商户详情。分类列表和 Redis GEO 附近商户查询使用各自的数据访问路径，不复用详情缓存。

## 读取链路

```text
Redisson Bloom Filter（cache:bloom:shop:v1）
  -> Caffeine L1（当前 JVM）
    -> Redis L2（cache:shop:{id}）
      -> MySQL
```

1. 布隆过滤器明确不存在时直接返回，不访问本地缓存、Redis 和 MySQL；过滤器异常时降级放行。
2. L1 命中直接返回，避免 Redis 网络访问和 JSON 序列化。
3. L1 未命中时，使用 Redis Cache-Aside：命中则回填 L1；未命中则读取 MySQL 并回填 Redis 与 L1。
4. 布隆过滤器可能误判，数据库不存在时仍在 Redis 写入空字符串，TTL 为 2 分钟；空对象不进入 L1。

默认 L2 商户缓存 TTL 为 30 分钟；L1 的 TTL 为 60 秒、最大容量 10000 条，可在 `application.yaml` 的 `hmdp.cache.shop.local` 下调整。

## 更新与失效链路

```text
更新 MySQL 事务
  -> 事务提交成功
    -> 清理当前实例 Caffeine L1
    -> 删除 Redis L2
    -> RocketMQ 广播 shop-cache-invalidation-topic
      -> 每个实例清理自己的 L1 和 Redis L2
```

`ShopCacheInvalidationListener` 使用 `@TransactionalEventListener(AFTER_COMMIT)`，因此不会发生“Redis 已删除但数据库事务尚未提交，读请求回填旧值”的窗口。

RocketMQ 消费者使用 `BROADCASTING` 模式：同一个消费者组内每个应用实例都会收到消息，这是本地缓存失效不能使用默认集群消费模式的原因。

## 失败边界

数据库提交后，缓存失效不会再影响数据库正确性。Redis 删除或消息发送失败会记录错误；L1 的短 TTL 和 Redis 的 TTL 会让旧数据最终自行过期。生产环境若要求消息发送不丢失，应再引入缓存失效 Outbox 表与定时重试，而不是仅依赖同步发送。

## 本地验证

1. 启动 MySQL、Redis、RocketMQ 与 Spring Boot。
2. 请求两次 `GET /shop/1`：第一次回源，第二次命中 L1。
3. 调用 `PUT /shop` 修改 ID 为 1 的商户。
4. 再请求 `GET /shop/1`，应读取更新后的数据；Redis 中的 `cache:shop:1` 会在提交后被删除并重新回填。
5. 若启动多个 Spring Boot 实例，观察每个实例的 Caffeine 缓存均因 RocketMQ 广播失效。

## 秒杀优惠券静态信息缓存

`GET /voucher/list/{shopId}` 按商户缓存优惠券静态信息，读取链路为：

```text
Caffeine L1（当前 JVM）
  -> Redis L2（cache:voucher:list:{shopId}）
    -> MySQL
```

缓存内容包含优惠券名称、规则、有效期和秒杀时间等静态字段，但不缓存秒杀库存。每次返回列表前，服务会批量读取 Redis 中的 `seckill:stock:{voucherId}`，把实时库存合并到响应中，避免本地缓存产生超卖判断依据或长时间展示旧库存。

默认 L1 TTL 为 60 秒、最大缓存商户数为 10000，Redis L2 TTL 为 10 分钟。新增或更新优惠券的数据库事务提交后，当前实例先清理 L1 和 Redis，再向 `voucher-cache-invalidation-topic` 发送 RocketMQ 广播消息；其他实例收到消息后清理各自的 L1，并幂等删除 Redis L2。

RocketMQ 或 Redis 短暂异常时会记录日志并降级；L1 短 TTL 与 Redis TTL 继续作为最终过期兜底。秒杀下单链路仍以 Redis Lua 中的实时库存校验为准，不依赖该静态信息缓存完成扣减。
