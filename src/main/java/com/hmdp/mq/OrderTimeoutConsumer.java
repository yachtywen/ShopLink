package com.hmdp.mq;

import com.hmdp.dto.OrderTimeoutMessage;
import com.hmdp.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static com.hmdp.utils.RocketMQConstants.CONSUMER_MAX_RECONSUME_TIMES;
import static com.hmdp.utils.RocketMQConstants.ORDER_TIMEOUT_CONSUMER_GROUP;
import static com.hmdp.utils.RocketMQConstants.ORDER_TIMEOUT_TOPIC;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = ORDER_TIMEOUT_TOPIC,
        consumerGroup = ORDER_TIMEOUT_CONSUMER_GROUP,
        maxReconsumeTimes = CONSUMER_MAX_RECONSUME_TIMES
)
public class OrderTimeoutConsumer implements RocketMQListener<OrderTimeoutMessage> {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @Override
    public void onMessage(OrderTimeoutMessage message) {
        try {
            log.debug("收到订单超时消息，message={}", message);
            voucherOrderService.handleOrderTimeout(message);
        } catch (Exception e) {
            log.error("订单超时消息消费失败，message={}", message, e);
            throw e;
        }
    }
}
