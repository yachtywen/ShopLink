package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.hmdp.dto.OrderTimeoutMessage;
import com.hmdp.dto.Result;
import com.hmdp.dto.SeckillOrderSubmitResult;
import com.hmdp.dto.VoucherOrderMessage;
import com.hmdp.dto.VoucherOrderStatusResult;
import com.hmdp.entity.MqMessage;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.MqMessageMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.mq.SeckillOrderMessageService;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;

import static com.hmdp.utils.MqMessageConstants.MQ_MESSAGE_STATUS_FAILED;
import static com.hmdp.utils.MqMessageConstants.MQ_MESSAGE_STATUS_SENT;
import static com.hmdp.utils.OrderConstants.ORDER_STATUS_CANCELLED;
import static com.hmdp.utils.OrderConstants.ORDER_STATUS_PAID;
import static com.hmdp.utils.OrderConstants.ORDER_STATUS_UNPAID;
import static com.hmdp.utils.OrderStatusViewConstants.VIEW_STATUS_CANCELLED;
import static com.hmdp.utils.OrderStatusViewConstants.VIEW_STATUS_FAILED;
import static com.hmdp.utils.OrderStatusViewConstants.VIEW_STATUS_PAID;
import static com.hmdp.utils.OrderStatusViewConstants.VIEW_STATUS_PENDING_PAY;
import static com.hmdp.utils.OrderStatusViewConstants.VIEW_STATUS_PROCESSING;
import static com.hmdp.utils.OrderStatusViewConstants.VIEW_STATUS_QUEUING;
import static com.hmdp.utils.OrderStatusViewConstants.VIEW_TEXT_CANCELLED;
import static com.hmdp.utils.OrderStatusViewConstants.VIEW_TEXT_FAILED;
import static com.hmdp.utils.OrderStatusViewConstants.VIEW_TEXT_PAID;
import static com.hmdp.utils.OrderStatusViewConstants.VIEW_TEXT_PENDING_PAY;
import static com.hmdp.utils.OrderStatusViewConstants.VIEW_TEXT_PROCESSING;
import static com.hmdp.utils.OrderStatusViewConstants.VIEW_TEXT_QUEUING;
import static com.hmdp.utils.RocketMQConstants.ORDER_TIMEOUT_DELAY_LEVEL;
import static com.hmdp.utils.RocketMQConstants.ORDER_TIMEOUT_TOPIC;
import static com.hmdp.utils.RocketMQConstants.SECKILL_ORDER_TOPIC;
import static com.hmdp.utils.RocketMQConstants.SEND_TIMEOUT_MILLIS;

@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    private static final String ORDER_LOCK_KEY_PREFIX = "lock:order:";

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    private static final DefaultRedisScript<Long> SECKILL_ROLLBACK_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);

        SECKILL_ROLLBACK_SCRIPT = new DefaultRedisScript<>();
        SECKILL_ROLLBACK_SCRIPT.setLocation(new ClassPathResource("seckill_rollback.lua"));
        SECKILL_ROLLBACK_SCRIPT.setResultType(Long.class);
    }

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private SeckillOrderMessageService seckillOrderMessageService;

    @Resource
    private MqMessageMapper mqMessageMapper;

    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        long orderId = redisIdWorker.nextId("order");

        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString()
        );
        int code = result == null ? 1 : result.intValue();
        if (code != 0) {
            return Result.fail(code == 1 ? "库存不足" : "不能重复下单");
        }

        VoucherOrderMessage message = new VoucherOrderMessage(orderId, userId, voucherId);
        try {
            seckillOrderMessageService.createAndSend(message);
        } catch (Exception e) {
            rollbackSeckillReservation(voucherId, userId);
            log.error("保存或投递秒杀订单消息失败，已回滚 Redis 预扣库存，voucherId={}, userId={}", voucherId, userId, e);
            return Result.fail("下单失败，请重试");
        }

        return Result.ok(new SeckillOrderSubmitResult(orderId, VIEW_STATUS_QUEUING, VIEW_TEXT_QUEUING));
    }

    @Override
    public Result queryOrderStatus(Long orderId) {
        VoucherOrder order = getById(orderId);
        if (order != null) {
            return Result.ok(orderStatusResult(order));
        }

        MqMessage mqMessage = mqMessageMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MqMessage>()
                .eq("order_id", orderId)
                .eq("topic", SECKILL_ORDER_TOPIC)
                .last("limit 1"));
        if (mqMessage == null) {
            return Result.fail("订单请求不存在");
        }
        if (Integer.valueOf(MQ_MESSAGE_STATUS_FAILED).equals(mqMessage.getStatus())) {
            return Result.ok(new VoucherOrderStatusResult(orderId, VIEW_STATUS_FAILED, VIEW_TEXT_FAILED, null, mqMessage.getCreateTime()));
        }
        if (Integer.valueOf(MQ_MESSAGE_STATUS_SENT).equals(mqMessage.getStatus())) {
            return Result.ok(new VoucherOrderStatusResult(orderId, VIEW_STATUS_PROCESSING, VIEW_TEXT_PROCESSING, null, mqMessage.getCreateTime()));
        }
        return Result.ok(new VoucherOrderStatusResult(orderId, VIEW_STATUS_QUEUING, VIEW_TEXT_QUEUING, null, mqMessage.getCreateTime()));
    }

    @Override
    @Transactional
    public void createVoucherOrder(VoucherOrderMessage message) {
        Long userId = message.getUserId();
        Long voucherId = message.getVoucherId();
        RLock redisLock = redissonClient.getLock(ORDER_LOCK_KEY_PREFIX + userId);
        boolean locked = redisLock.tryLock();
        if (!locked) {
            throw new IllegalStateException("重复订单正在处理中，稍后重试");
        }

        try {
            VoucherOrder existing = query()
                    .eq("user_id", userId)
                    .eq("voucher_id", voucherId)
                    .one();
            if (existing != null) {
                if (Integer.valueOf(ORDER_STATUS_UNPAID).equals(existing.getStatus())) {
                    sendOrderTimeoutMessage(existing.getId(), userId, voucherId);
                }
                return;
            }

            boolean stockDeducted = seckillVoucherService.update()
                    .setSql("stock = stock - 1")
                    .eq("voucher_id", voucherId)
                    .gt("stock", 0)
                    .update();
            if (!stockDeducted) {
                rollbackSeckillReservation(voucherId, userId);
                markOrderMessageFailed(message.getOrderId(), "MySQL 库存不足");
                log.error("MySQL 库存不足，已回滚 Redis 预扣库存，voucherId={}, userId={}", voucherId, userId);
                return;
            }

            VoucherOrder voucherOrder = new VoucherOrder();
            voucherOrder.setId(message.getOrderId());
            voucherOrder.setUserId(userId);
            voucherOrder.setVoucherId(voucherId);
            voucherOrder.setStatus(ORDER_STATUS_UNPAID);
            try {
                save(voucherOrder);
            } catch (DuplicateKeyException e) {
                seckillVoucherService.update()
                        .setSql("stock = stock + 1")
                        .eq("voucher_id", voucherId)
                        .update();
                log.warn("重复订单消息已被数据库唯一索引拦截，userId={}, voucherId={}", userId, voucherId);
                return;
            }

            sendOrderTimeoutMessage(message.getOrderId(), userId, voucherId);
        } finally {
            redisLock.unlock();
        }
    }

    @Override
    @Transactional
    public void handleOrderTimeout(OrderTimeoutMessage message) {
        boolean cancelled = update()
                .set("status", ORDER_STATUS_CANCELLED)
                .eq("id", message.getOrderId())
                .eq("status", ORDER_STATUS_UNPAID)
                .update();
        if (!cancelled) {
            return;
        }

        seckillVoucherService.update()
                .setSql("stock = stock + 1")
                .eq("voucher_id", message.getVoucherId())
                .update();
        rollbackSeckillReservation(message.getVoucherId(), message.getUserId());
        System.out.println("订单超时");
    }

    @Override
    @Transactional
    public Result mockPay(Long orderId, Boolean success) {
        VoucherOrder order = getById(orderId);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (!Boolean.TRUE.equals(success)) {
            System.out.println("支付失败");
            return Result.ok("支付失败");
        }

        boolean paid = update()
                .set("status", ORDER_STATUS_PAID)
                .set("pay_time", LocalDateTime.now())
                .eq("id", orderId)
                .eq("status", ORDER_STATUS_UNPAID)
                .update();
        if (!paid) {
            return Result.fail("订单状态不允许支付");
        }

        System.out.println("支付成功");
        return Result.ok("支付成功");
    }

    private VoucherOrderStatusResult orderStatusResult(VoucherOrder order) {
        Integer status = order.getStatus();
        if (Integer.valueOf(ORDER_STATUS_PAID).equals(status)) {
            return new VoucherOrderStatusResult(order.getId(), VIEW_STATUS_PAID, VIEW_TEXT_PAID, order.getPayTime(), order.getCreateTime());
        }
        if (Integer.valueOf(ORDER_STATUS_CANCELLED).equals(status)) {
            return new VoucherOrderStatusResult(order.getId(), VIEW_STATUS_CANCELLED, VIEW_TEXT_CANCELLED, order.getPayTime(), order.getCreateTime());
        }
        return new VoucherOrderStatusResult(order.getId(), VIEW_STATUS_PENDING_PAY, VIEW_TEXT_PENDING_PAY, order.getPayTime(), order.getCreateTime());
    }

    private void sendOrderTimeoutMessage(Long orderId, Long userId, Long voucherId) {
        OrderTimeoutMessage timeoutMessage = new OrderTimeoutMessage(orderId, userId, voucherId);
        rocketMQTemplate.syncSend(
                ORDER_TIMEOUT_TOPIC,
                MessageBuilder.withPayload(timeoutMessage).build(),
                SEND_TIMEOUT_MILLIS,
                ORDER_TIMEOUT_DELAY_LEVEL
        );
    }

    private void markOrderMessageFailed(Long orderId, String reason) {
        mqMessageMapper.update(null, new UpdateWrapper<MqMessage>()
                .eq("order_id", orderId)
                .eq("topic", SECKILL_ORDER_TOPIC)
                .set("status", MQ_MESSAGE_STATUS_FAILED)
                .set("last_error", reason));
    }

    private void rollbackSeckillReservation(Long voucherId, Long userId) {
        stringRedisTemplate.execute(
                SECKILL_ROLLBACK_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString()
        );
    }
}
