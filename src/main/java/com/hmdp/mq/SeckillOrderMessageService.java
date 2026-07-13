package com.hmdp.mq;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.hmdp.dto.VoucherOrderMessage;
import com.hmdp.entity.MqMessage;
import com.hmdp.mapper.MqMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static com.hmdp.utils.MqMessageConstants.MQ_MESSAGE_DEFAULT_MAX_RETRY_COUNT;
import static com.hmdp.utils.MqMessageConstants.MQ_MESSAGE_INITIAL_RETRY_DELAY_SECONDS;
import static com.hmdp.utils.MqMessageConstants.MQ_MESSAGE_STATUS_FAILED;
import static com.hmdp.utils.MqMessageConstants.MQ_MESSAGE_STATUS_SENDING;
import static com.hmdp.utils.MqMessageConstants.MQ_MESSAGE_STATUS_SENT;
import static com.hmdp.utils.RocketMQConstants.SECKILL_ORDER_TOPIC;
import static com.hmdp.utils.RocketMQConstants.SEND_TIMEOUT_MILLIS;

@Slf4j
@Component
public class SeckillOrderMessageService {

    private static final int RETRY_BATCH_SIZE = 20;
    private static final int MAX_ERROR_LENGTH = 1024;
    private static final DefaultRedisScript<Long> SECKILL_ROLLBACK_SCRIPT;

    static {
        SECKILL_ROLLBACK_SCRIPT = new DefaultRedisScript<>();
        SECKILL_ROLLBACK_SCRIPT.setLocation(new ClassPathResource("seckill_rollback.lua"));
        SECKILL_ROLLBACK_SCRIPT.setResultType(Long.class);
    }

    @Resource
    private MqMessageMapper mqMessageMapper;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void createAndSend(VoucherOrderMessage message) {
        MqMessage mqMessage = new MqMessage();
        mqMessage.setOrderId(message.getOrderId());
        mqMessage.setUserId(message.getUserId());
        mqMessage.setVoucherId(message.getVoucherId());
        mqMessage.setTopic(SECKILL_ORDER_TOPIC);
        mqMessage.setMessageBody(JSONUtil.toJsonStr(message));
        mqMessage.setStatus(MQ_MESSAGE_STATUS_SENDING);
        mqMessage.setRetryCount(0);
        mqMessage.setMaxRetryCount(MQ_MESSAGE_DEFAULT_MAX_RETRY_COUNT);
        mqMessage.setNextRetryTime(LocalDateTime.now().plusSeconds(MQ_MESSAGE_INITIAL_RETRY_DELAY_SECONDS));
        mqMessageMapper.insert(mqMessage);

        sendAsync(mqMessage, message);
    }

    public void retryPendingMessages() {
        List<MqMessage> messages = mqMessageMapper.selectList(new QueryWrapper<MqMessage>()
                .eq("topic", SECKILL_ORDER_TOPIC)
                .eq("status", MQ_MESSAGE_STATUS_SENDING)
                .le("next_retry_time", LocalDateTime.now())
                .apply("retry_count < max_retry_count")
                .last("limit " + RETRY_BATCH_SIZE));
        for (MqMessage mqMessage : messages) {
            VoucherOrderMessage message = JSONUtil.toBean(mqMessage.getMessageBody(), VoucherOrderMessage.class);
            sendAsync(mqMessage, message);
        }
    }

    private void sendAsync(MqMessage mqMessage, VoucherOrderMessage message) {
        try {
            rocketMQTemplate.asyncSend(
                    SECKILL_ORDER_TOPIC,
                    MessageBuilder.withPayload(message).build(),
                    new SendCallback() {
                        @Override
                        public void onSuccess(SendResult sendResult) {
                            markSent(mqMessage.getId(), sendResult.getMsgId());
                        }

                        @Override
                        public void onException(Throwable throwable) {
                            markSendFailed(mqMessage, throwable);
                        }
                    },
                    SEND_TIMEOUT_MILLIS
            );
        } catch (Exception e) {
            markSendFailed(mqMessage, e);
        }
    }

    private void markSent(Long id, String messageId) {
        int updated = mqMessageMapper.update(null, new UpdateWrapper<MqMessage>()
                .eq("id", id)
                .eq("status", MQ_MESSAGE_STATUS_SENDING)
                .set("message_id", messageId)
                .set("status", MQ_MESSAGE_STATUS_SENT)
                .set("last_error", null));
        if (updated > 0) {
            log.debug("秒杀订单消息发送成功，mqMessageId={}, rocketmqMessageId={}", id, messageId);
        }
    }

    private void markSendFailed(MqMessage mqMessage, Throwable throwable) {
        int retryCount = safeRetryCount(mqMessage.getRetryCount()) + 1;
        int maxRetryCount = safeMaxRetryCount(mqMessage.getMaxRetryCount());
        String error = normalizeError(throwable);

        if (retryCount >= maxRetryCount) {
            int updated = mqMessageMapper.update(null, new UpdateWrapper<MqMessage>()
                    .eq("id", mqMessage.getId())
                    .eq("status", MQ_MESSAGE_STATUS_SENDING)
                    .set("status", MQ_MESSAGE_STATUS_FAILED)
                    .set("retry_count", retryCount)
                    .set("last_error", error));
            if (updated > 0) {
                rollbackSeckillReservation(mqMessage.getVoucherId(), mqMessage.getUserId());
                log.error("秒杀订单消息发送重试耗尽，已回滚 Redis 预扣库存，mqMessageId={}, orderId={}",
                        mqMessage.getId(), mqMessage.getOrderId(), throwable);
            }
            return;
        }

        LocalDateTime nextRetryTime = LocalDateTime.now().plusSeconds(nextRetryDelaySeconds(retryCount));
        mqMessageMapper.update(null, new UpdateWrapper<MqMessage>()
                .eq("id", mqMessage.getId())
                .eq("status", MQ_MESSAGE_STATUS_SENDING)
                .set("retry_count", retryCount)
                .set("next_retry_time", nextRetryTime)
                .set("last_error", error));
        log.warn("秒杀订单消息发送失败，等待补偿重试，mqMessageId={}, retryCount={}, nextRetryTime={}",
                mqMessage.getId(), retryCount, nextRetryTime, throwable);
    }

    private int safeRetryCount(Integer retryCount) {
        return retryCount == null ? 0 : retryCount;
    }

    private int safeMaxRetryCount(Integer maxRetryCount) {
        return maxRetryCount == null ? MQ_MESSAGE_DEFAULT_MAX_RETRY_COUNT : maxRetryCount;
    }

    private long nextRetryDelaySeconds(int retryCount) {
        int shift = Math.min(retryCount, 4);
        return Math.min(60L, 5L * (1L << shift));
    }

    private String normalizeError(Throwable throwable) {
        String message = throwable == null ? "unknown error" : throwable.toString();
        if (message.length() <= MAX_ERROR_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_ERROR_LENGTH);
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
