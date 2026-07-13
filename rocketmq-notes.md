# RocketMQ 秒杀改造笔记

## 当前项目链路

当前秒杀链路是：

```text
用户秒杀请求
  -> Lua 判断库存、一人一单
  -> Redis 预扣库存
  -> Redis Stream 写入订单消息
  -> 本地 ExecutorService 消费 Stream
  -> 创建订单
```

这个方案适合课程学习，但生产化有几个问题：

```text
本地线程池和服务实例绑定太强
消费者扩容能力弱
异常重试、死信、补偿能力偏弱
服务重启/消费组维护需要自己处理
不方便扩展订单超时取消
```

## 推荐技术选型

建议：

```text
核心秒杀订单消息：RocketMQ
秒杀资格判断和预扣库存：Redis + Lua 保留
分布式锁：Redisson 可保留，但数据库唯一索引更关键
数据库兜底：user_id + voucher_id 唯一索引
超时未支付关闭订单：RocketMQ 延迟消息
日志/行为分析：后续再考虑 Kafka，不建议第一版引入
```

为什么不是 Kafka：

```text
Kafka 更适合日志、行为事件、实时统计、大数据分析
RocketMQ 更适合订单、交易、延迟消息、失败重试、死信队列
```

这个项目第一目标是“秒杀下单可靠落库”，所以 RocketMQ 更贴合。

## 优化点 1：替换 Redis Stream 消费

把当前的：

```text
Redis Stream + ExecutorService + while(true) read
```

替换成：

```text
RocketMQ Producer + RocketMQ Consumer
```

改造后：

```text
用户请求
  -> 执行 Lua
  -> 同步发送订单消息到 RocketMQ
  -> 返回订单 id

RocketMQ 消费者
  -> 监听订单 topic
  -> 创建订单
  -> 成功 ACK
  -> 失败交给 RocketMQ 重试
```

第一版建议使用同步发送，不要先用异步发送或单向发送。

## 优化点 2：处理 Redis 成功但 MQ 发送失败

这是改造里最重要的坑。

因为 Lua 不能直接发 RocketMQ，你会变成：

```text
Lua 成功扣 Redis 库存
Java 再发送 MQ 消息
```

如果 MQ 发送失败，就会出现：

```text
Redis 库存扣了
用户也被加入已下单 Set
但订单消息没发出去
```

第一版可以做补偿：

```text
MQ 发送失败
  -> Redis 库存 +1
  -> 从 seckill:order:{voucherId} 删除 userId
  -> 返回下单失败，请重试
```

更高级方案是 RocketMQ 事务消息，但第一版不建议上来就做，复杂度更高。

## 优化点 3：消费者幂等

消费者必须保证同一条订单消息消费多次也不会重复下单。

建议三层：

```text
1. 订单 ID 唯一
2. user_id + voucher_id 唯一索引
3. 消费者里仍然查一次是否已下单
```

因为 RocketMQ 是“至少消费一次”，消息可能重复投递。

消费者逻辑：

```text
收到订单消息
  -> 根据 orderId 或 userId+voucherId 判断是否已存在
  -> 已存在：直接返回消费成功
  -> 不存在：扣数据库库存并保存订单
```

注意：重复消息应该返回消费成功，不要返回失败，否则会一直重试。

## 优化点 4：订单超时未支付

如果后续把业务扩展成“秒杀后要支付”，可以用 RocketMQ 延迟消息。

流程：

```text
创建订单成功，状态 = 待支付
  -> 发送 15 分钟延迟消息

15 分钟后消费者收到消息
  -> 查询订单状态
  -> 如果还是待支付
       -> 关闭订单
       -> 释放库存
  -> 如果已支付
       -> 忽略
```

这比定时任务扫数据库更精准，也更适合订单量大的场景。

## 优化点 5：死信队列和补偿任务

如果订单消息重试很多次仍然失败，会进入死信队列。

需要准备一个补偿思路：

```text
死信消息
  -> 记录告警
  -> 人工或定时补偿
  -> 根据 orderId / userId / voucherId 查 Redis 和数据库状态
  -> 决定补单或回滚资格
```

第一版不一定要做完整后台页面，但至少要知道这类消息不能无视。

## 大概改造步骤

1. 引入 RocketMQ Spring Boot Starter。
2. 配置 `name-server`、生产者 group、消费者 group。
3. 定义订单消息 DTO，比如 `VoucherOrderMessage`。
4. 修改秒杀方法：
   - 执行 Lua。
   - Lua 成功后同步发送 RocketMQ 消息。
   - 发送失败则补偿 Redis。
5. 新增 RocketMQ 消费者：
   - 监听 `seckill-order-topic`。
   - 调用创建订单逻辑。
   - 成功返回 ACK。
   - 异常交给 RocketMQ 重试。
6. 数据库加唯一索引：
   - `user_id + voucher_id`。
7. 删除或停用原来的：
   - `ExecutorService`。
   - `VoucherOrderHandler`。
   - Redis Stream 消费逻辑。
8. 保留 Redis Lua：
   - 继续负责高并发下的库存预扣和一人一单前置判断。

## 推荐最终架构

```text
请求线程
  -> Redis Lua 原子判断资格
  -> 同步发送 RocketMQ 订单消息
  -> 返回 orderId

RocketMQ
  -> 持久化订单消息
  -> 失败自动重试
  -> 多次失败进入死信队列

消费者
  -> 幂等校验
  -> 扣 MySQL 库存
  -> 保存订单
  -> 返回消费成功
```

## 结论

当前业务最值得用 MQ 优化的是“异步创建秒杀订单”和“订单超时关闭”。

第一版建议：

```text
RocketMQ
同步发送
消费失败重试
数据库唯一索引兜底
消费者幂等
Redis Lua 保留
```

先把可靠性做扎实，再考虑异步发送、事务消息、Kafka 日志链路等扩展。

## 后续计划
1.死信队列实现。
2.
3.redis和mysql的连接
4.延迟队列解决超时订单
5.redis AOP 注解滑动窗口限流。
6.mq事务机制解决缓存扣减和数据库落库最终一致性
请你总结我本次对话中对当前dianping项目的优化内容，并且帮我总结出两三点可以放在简历里的项目亮点，还要写一些项目整体的亮点。