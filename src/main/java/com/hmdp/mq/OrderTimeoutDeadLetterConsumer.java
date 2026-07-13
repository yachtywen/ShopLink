package com.hmdp.mq;

import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static com.hmdp.utils.RocketMQConstants.ORDER_TIMEOUT_CONSUMER_GROUP;
import static com.hmdp.utils.RocketMQConstants.ORDER_TIMEOUT_DLQ_CONSUMER_GROUP;
import static com.hmdp.utils.RocketMQConstants.ORDER_TIMEOUT_DLQ_TOPIC;

@Component
@RocketMQMessageListener(topic = ORDER_TIMEOUT_DLQ_TOPIC, consumerGroup = ORDER_TIMEOUT_DLQ_CONSUMER_GROUP)
public class OrderTimeoutDeadLetterConsumer implements RocketMQListener<MessageExt> {

    @Resource
    private DeadLetterMessageRecorder deadLetterMessageRecorder;

    @Override
    public void onMessage(MessageExt message) {
        deadLetterMessageRecorder.record(message, ORDER_TIMEOUT_CONSUMER_GROUP);
    }
}
