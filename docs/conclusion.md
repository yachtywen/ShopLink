# hm-dianping 项目优化总结

## 本轮对话完成的优化内容

本轮主要围绕秒杀订单链路做了 MQ 化和可靠性增强，将原有 Redis Stream/线程池式异步下单思路升级为 RocketMQ 方案，并进一步从同步发送演进到带本地消息表的异步发送方案。

核心改动包括：

1. 引入 RocketMQ 作为秒杀订单消息队列
   - 使用 Docker Compose 增加 RocketMQ NameServer、Broker、Dashboard。
   - 修复 Broker 注册地址问题，将 `brokerIP1` 调整为 `host.docker.internal`，解决 Dashboard 访问 Broker 报 `127.0.0.1:10909 failed` 的问题。
   - 项目中增加 RocketMQ Spring Boot Starter，并配置 `rocketmq.name-server` 和 Producer group。

2. 重构秒杀下单链路
   - 保留 Redis + Lua 做秒杀资格判断、库存预扣和一人一单前置校验。
   - 删除旧 Redis Stream 消息队列实现，不再使用 `stream.orders`、`opsForStream` 和后台单线程消费模型。
   - 使用 RocketMQ topic 承接秒杀订单消息，消费者异步创建 MySQL 订单。
   - Consumer 侧保留 Redisson 用户锁、数据库幂等查询和 `user_id + voucher_id` 唯一索引兜底，防止重复下单。

3. 增加本地消息表和异步发送补偿机制
   - 新增 `tb_mq_message`，记录 Producer 到 Broker 的投递状态。
   - 秒杀接口改为先写本地消息表，再使用 `rocketMQTemplate.asyncSend()` 异步发送消息。
   - 发送成功回调将消息状态改为 `SENT`。
   - 发送失败回调记录错误、重试次数和下次重试时间。
   - 新增定时补偿任务，扫描 `SENDING` 消息并重新投递。
   - 重试耗尽后将消息标记为 `FAILED`，并回滚 Redis 预扣库存和用户下单 Set。

4. 增加订单状态查询能力
   - 新增 `GET /voucher-order/status/{id}`。
   - 秒杀接口返回 `orderId + 排队中`，前端可通过状态查询接口查看后续结果。
   - 状态覆盖：`排队中`、`处理中`、`待支付`、`支付成功`、`已取消`、`处理失败`。
   - 明确了支付前后状态语义：`待支付` 是订单已创建但未支付，支付成功后才变为 `支付成功`。

5. 增加超时未支付关闭订单
   - 订单创建成功后发送 RocketMQ 延迟消息。
   - 延迟消息 5 分钟后投递，若订单仍是未支付状态，则关闭订单、回补 MySQL 库存、回补 Redis 库存并移除用户下单 Set。
   - 若用户已支付，延迟消息仍会到达，但只做状态判断，不会取消订单或回补库存。

6. 增加死信消息记录
   - 显式配置 Consumer 最大重试次数。
   - 新增两个 DLQ 消费者，分别监听秒杀订单和订单超时消费者对应的死信 topic。
   - 新增 `tb_dead_letter_message`，用于记录死信消息内容、Consumer Group、重试次数等信息。
   - 当前死信消息只做异常消费记录，不做库存回滚、订单创建、消息重投递或自动业务补偿。

7. 完成本地运行环境与数据库初始化
   - 新增 `dianping` 数据库初始化 SQL 和增量 SQL。
   - 新增 `tb_mq_message`、`tb_dead_letter_message`、`tb_voucher_order` 唯一索引等结构。
   - 新增 Redis 初始化脚本，用于初始化秒杀库存、清理下单 Set、写入商户 GEO。
   - 校正本地 MySQL Docker 实际配置：`127.0.0.1:3307`，root 密码 `123456`。

## 当前秒杀链路设计

当前秒杀链路整体流程如下：

```text
用户请求秒杀
  -> Redis Lua 原子校验库存和一人一单
  -> Redis 预扣库存并记录用户下单资格
  -> 写入 tb_mq_message，状态为 SENDING
  -> Producer 异步发送 RocketMQ 消息
  -> 发送成功后消息状态改为 SENT
  -> Consumer 消费消息并创建 MySQL 订单
  -> 创建成功后发送 5 分钟延迟消息
  -> 用户 mock 支付或等待超时关闭
```

异常兜底逻辑：

```text
Producer 发送失败
  -> 本地消息表保留 SENDING
  -> 定时任务补偿重试
  -> 重试耗尽后标记 FAILED 并回滚 Redis

Consumer 消费失败
  -> 抛异常
  -> RocketMQ 自动重试
  -> 重试耗尽进入 DLQ
  -> DLQ 消费者只记录异常消费

订单超时未支付
  -> 延迟消息触发
  -> 仅当订单仍是未支付才关闭
  -> 回补 Redis 和 MySQL 库存
```

## 可写进简历的项目亮点

### 亮点 1：基于 Redis + Lua + RocketMQ 构建高并发秒杀下单链路

可写法：

> 负责秒杀下单链路优化，使用 Redis + Lua 实现库存预扣和一人一单原子校验，引入 RocketMQ 解耦下单请求与订单落库流程，降低秒杀高峰期数据库写入压力，并通过数据库唯一索引和消费者幂等校验防止重复下单。

体现能力：

- 高并发削峰
- Redis 原子操作
- MQ 异步解耦
- 幂等设计
- 数据库兜底约束

### 亮点 2：设计本地消息表 + 定时补偿机制，提升 MQ 投递可靠性

可写法：

> 针对 Producer 到 Broker 投递失败场景，设计并实现本地消息表 `tb_mq_message`，记录消息发送状态、重试次数和失败原因，结合定时补偿任务对待发送消息进行重投递，重试耗尽后自动回滚 Redis 预扣库存，提升 Redis 缓存扣减与 MySQL 订单落库的最终一致性。

体现能力：

- 最终一致性设计
- 可靠消息投递
- 本地消息表模式
- 失败补偿机制
- 状态机思维

### 亮点 3：引入延迟消息与死信记录完善订单异常处理

可写法：

> 使用 RocketMQ 延迟消息实现订单 5 分钟未支付自动关闭，关闭时回补 MySQL 和 Redis 库存；同时接入 RocketMQ 死信队列监听，将消费重试耗尽的异常消息落库记录，便于后续排查和人工补偿。

体现能力：

- 延迟消息应用
- 超时订单关闭
- 消费失败重试
- 死信消息治理
- 可观测性和问题排查

## 项目整体亮点

1. 用户登录与会话管理
   - 使用 Redis 存储短信验证码和登录 token。
   - 通过拦截器完成 token 刷新、登录态校验和用户上下文保存。
   - 适合体现 Redis 在登录态管理中的应用。

2. 商户缓存优化
    - 商户详情采用 Cache-Aside：Redis 未命中时回源 MySQL 并回填；不存在的商户缓存空值 2 分钟，防止缓存穿透。
    - 商户更新后通过 `@TransactionalEventListener(AFTER_COMMIT)` 清理缓存，避免数据库未提交时缓存已删除、读请求回填旧值的并发窗口。
    - 已升级为 Caffeine L1 + Redis L2 两级缓存，仅对热点商户详情生效，减少 Redis 网络访问和 JSON 序列化开销。
    - 使用 RocketMQ 广播缓存失效事件，使多应用实例均能清理自己的本地缓存；L1 短 TTL 和 Redis TTL 提供失败后的最终自愈。

3. 秒杀订单高并发设计
   - 使用 Redis 预扣库存处理高并发请求。
   - 使用 RocketMQ 削峰填谷，将数据库写入压力转移到消费者侧平稳处理。
   - 使用本地消息表、消费者重试、死信记录和唯一索引提升链路可靠性。

4. 分布式锁与幂等控制
   - 使用 Redisson 对同一用户下单逻辑加锁。
   - 使用数据库唯一索引作为最终一致性兜底。
   - 适合体现并发安全和分布式场景下的一人一单设计。

5. 本地容器化运行环境
   - Redis、MySQL、RocketMQ 均可通过 Docker 本地运行。
   - 补充了数据库初始化 SQL、Redis 初始化脚本和 RocketMQ Docker Compose 配置。
    - 有利于项目复现、调试和后续继续扩展。

6. 接口测试前端与 Nginx 部署链路
    - 新增 Vue 3 + TypeScript + Vite + Element Plus 前端测试台，覆盖登录、商户、笔记、关注、优惠券秒杀等主要接口验证场景。
    - 使用 Nginx 托管前端静态资源、代理 `/api` 到 Spring Boot、映射 `/imgs` 到上传图片目录，并通过 `try_files` 解决 Vue History 路由刷新 404。
    - 前端统一封装 Axios Token 注入、401 登录态清理、业务异常提示和路由权限控制。

7. Redis + AOP 多维滑动窗口限流
    - 基于 Redis ZSET 和 Lua 实现原子滑动窗口限流，支持全局、IP、登录用户三种维度。
    - 通过注解保护验证码、登录、秒杀、图片上传、发布笔记、商户搜索与热门笔记等高风险接口。
    - 任一维度超限统一返回 HTTP 429、`Retry-After` 和项目统一 `Result` 响应；可显式配置是否信任 Nginx 转发 IP 头。

## 本轮新增：缓存一致性与两级缓存设计

### 1. Redis Cache-Aside 商户详情缓存

商户详情读取流程：

```text
请求 GET /shop/{id}
  -> Caffeine L1（当前 JVM）
  -> Redis L2（cache:shop:{id}）
  -> MySQL
  -> 回填 Redis 与 Caffeine
```

- Redis 中命中有效商户数据时直接返回。
- Redis 未命中时查询 MySQL，并将非空数据写入 Redis，TTL 为 30 分钟。
- MySQL 未找到商户时，在 Redis 写入空字符串，TTL 为 2 分钟；空值不写入 Caffeine，避免本地缓存占用无效数据。

### 2. 事务提交后缓存失效

商户更新流程：

```text
更新 MySQL 事务
  -> 事务成功提交
  -> 清理当前实例 Caffeine L1
  -> 删除 Redis L2
  -> RocketMQ 广播缓存失效消息
  -> 所有应用实例清理本地缓存
```

- `ShopServiceImpl.update()` 只在数据库更新成功后发布缓存失效事件。
- `ShopCacheInvalidationListener` 使用 `@TransactionalEventListener(phase = AFTER_COMMIT)`，确保缓存失效在事务真正提交后执行。
- RocketMQ Consumer 使用 `BROADCASTING`，而非默认集群消费；这样每个 JVM 都能收到消息并清理各自的 Caffeine 缓存。
- 当前版本在 Redis 删除或 MQ 发送失败时记录错误，依赖 L1 60 秒 TTL 与 Redis 30 分钟 TTL 最终自愈。若生产环境要求严格的消息可靠投递，应继续增加缓存失效 Outbox 表和定时补偿。

相关实现与说明：

- `src/main/java/com/hmdp/cache/ShopCacheService.java`
- `src/main/java/com/hmdp/cache/ShopCacheInvalidationListener.java`
- `src/main/java/com/hmdp/cache/ShopCacheInvalidationConsumer.java`
- `docs/CACHE_ARCHITECTURE.md`

### 3. 可写进简历的缓存亮点

> 针对热点商户详情构建 Caffeine + Redis 两级 Cache-Aside 缓存：本地缓存优先承接热点请求，Redis 作为分布式二级缓存；缓存未命中回源 MySQL 并缓存空值防穿透。商户更新通过事务提交后双层缓存失效，并使用 RocketMQ 广播同步多实例本地缓存，保障数据最终一致性。

### 4. 构建兼容性修复

- 当前本机使用 JDK 17；原 Lombok `1.18.20` 无法正常进行注解处理，导致项目中的实体和 DTO 缺少 getter/setter 而编译失败。
- 已将 Lombok 升级到 `1.18.32`，恢复 JDK 17 下的编译能力。
- 已通过定向测试：`ShopCacheServiceTest` 验证 L1 命中优先级以及 L1/L2 清理后重新回源的行为。

## 面试讲解建议

如果面试官问“这个秒杀系统如何保证不超卖”，可以按这个顺序讲：

```text
第一层：Redis Lua 原子判断库存并预扣，挡住大部分高并发请求。
第二层：Redis Set 记录用户是否下单，前置保证一人一单。
第三层：Consumer 侧使用 Redisson 用户锁，避免同一用户消息并发处理。
第四层：MySQL 扣库存时加 stock > 0 条件，数据库层防超卖。
第五层：user_id + voucher_id 唯一索引，最终防重复订单。
```

如果面试官问“Redis 扣了库存但订单没创建怎么办”，可以这样讲：

```text
Redis Lua 成功后会写入本地消息表，消息状态为 SENDING。
如果 MQ 发送失败，定时补偿任务会扫描 SENDING 消息并重发。
如果重试耗尽，会将消息标记为 FAILED，并回滚 Redis 库存和用户下单 Set。
如果 MQ 已经发送成功但消费者失败，则依赖 RocketMQ 消费重试。
超过消费重试次数后进入死信队列，当前版本只记录死信消息，后续可扩展人工补偿。
```

如果面试官问“为什么支付成功后延迟消息还会执行”，可以这样讲：

```text
RocketMQ 延迟消息发出后不会因为支付成功而自动取消。
延迟消息仍会在 5 分钟后投递，但消费者会先检查订单状态。
只有订单仍是未支付状态才会关闭订单并回补库存。
如果订单已经支付成功，状态判断不命中，消息直接忽略。
这是典型的基于状态判断的幂等处理。
```

## 后续可继续优化方向

1. 增加死信补偿后台
   - 当前死信消息只记录异常消费。
   - 后续可以增加人工审核、重新投递、库存修复等补偿入口。

2. 增加完整订单状态机
   - 当前只覆盖秒杀请求投递状态和订单支付状态。
   - 后续可扩展为完整状态机：`ACCEPTED`、`SENDING`、`SENT`、`CREATING`、`CREATED`、`PAID`、`CANCELLED`、`FAILED`。

3. 优化 Redis 预扣后的极端宕机问题
   - 当前理论极端点：Redis Lua 成功后、写本地消息表前应用宕机，会出现预扣无本地记录。
   - 后续可以通过更完整的资格冻结记录、事务边界调整或可靠事件设计继续增强。

4. 增加压测和监控
   - 使用 JMeter 或 wrk 对秒杀接口压测。
   - 增加 MQ 堆积、消费失败、补偿重试次数、死信数量等指标监控。

5. 完善支付链路
   - 当前支付为 mock。
   - 后续可以接入真实支付回调、支付幂等、支付超时关闭和退款状态流转。

## 当前运行注意事项

本地 MySQL Docker 容器 `mysql8` 当前实际连接信息：

```text
host: 127.0.0.1
port: 3307
username: root
password: 123456
database: dianping
```

RocketMQ Dashboard：

```text
http://127.0.0.1:8088
```

已有数据库需要执行：

```text
src/main/resources/db/dianping_incremental.sql
```

新建数据库可执行：

```text
src/main/resources/db/dianping.sql
```

项目编译验证命令：

```powershell
mvn -q -gs D:\tmp\maven-settings-aliyun.xml -s D:\tmp\maven-settings-aliyun.xml "-Dmaven.repo.local=D:\tmp\m2-hmdp" compile
```

## ToDo
1.JMeter压测
2.缓存失效 Outbox 表 + 定时补偿，增强数据库提交后 RocketMQ 广播失败时的可靠投递能力。
3.理解延迟队列解决超时订单和我现有实现方式的区别
4.复盘 分布式锁、缓存击穿，穿透，雪崩以及项目的预防方式
5.了解rocketmq的事务消息
6.了解JWT
7.点评优化里支付和关单的并发
8.启动多个 Spring Boot 实例，实际验证 RocketMQ 广播模式下的 Caffeine 本地缓存同步失效。
