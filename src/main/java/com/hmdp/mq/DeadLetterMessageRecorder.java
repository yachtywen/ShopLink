package com.hmdp.mq;

import com.hmdp.entity.DeadLetterMessage;
import com.hmdp.mapper.DeadLetterMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class DeadLetterMessageRecorder {

    private static final int MAX_BODY_LENGTH = 4096;
    private static final int MAX_ERROR_LENGTH = 1024;

    @Resource
    private DeadLetterMessageMapper deadLetterMessageMapper;

    public void record(MessageExt message, String consumerGroup) {
        DeadLetterMessage deadLetterMessage = new DeadLetterMessage();
        deadLetterMessage.setTopic(message.getTopic());
        deadLetterMessage.setConsumerGroup(consumerGroup);
        deadLetterMessage.setMessageBody(limit(new String(message.getBody(), StandardCharsets.UTF_8), MAX_BODY_LENGTH));
        deadLetterMessage.setErrorMessage(limit("Message entered RocketMQ DLQ after consumer retries were exhausted", MAX_ERROR_LENGTH));
        deadLetterMessage.setReconsumeTimes(message.getReconsumeTimes());
        deadLetterMessageMapper.insert(deadLetterMessage);
        log.error("记录 RocketMQ 死信消息，topic={}, consumerGroup={}, msgId={}, reconsumeTimes={}",
                message.getTopic(), consumerGroup, message.getMsgId(), message.getReconsumeTimes());
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
