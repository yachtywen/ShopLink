package com.hmdp.mq;

import com.hmdp.dto.VoucherOrderMessage;
import com.hmdp.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static com.hmdp.utils.RocketMQConstants.CONSUMER_MAX_RECONSUME_TIMES;
import static com.hmdp.utils.RocketMQConstants.SECKILL_ORDER_CONSUMER_GROUP;
import static com.hmdp.utils.RocketMQConstants.SECKILL_ORDER_TOPIC;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = SECKILL_ORDER_TOPIC,
        consumerGroup = SECKILL_ORDER_CONSUMER_GROUP,
        maxReconsumeTimes = CONSUMER_MAX_RECONSUME_TIMES
)
public class SeckillOrderConsumer implements RocketMQListener<VoucherOrderMessage> {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @Override
    public void onMessage(VoucherOrderMessage message) {
        try {
            log.debug("收到秒杀订单消息，message={}", message);
            voucherOrderService.createVoucherOrder(message);
        } catch (Exception e) {
            log.error("秒杀订单消息消费失败，message={}", message, e);
            throw e;
        }
    }
}
