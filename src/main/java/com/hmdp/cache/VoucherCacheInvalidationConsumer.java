package com.hmdp.cache;

import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static com.hmdp.utils.RocketMQConstants.VOUCHER_CACHE_INVALIDATION_CONSUMER_GROUP;
import static com.hmdp.utils.RocketMQConstants.VOUCHER_CACHE_INVALIDATION_TOPIC;

@Component
@RocketMQMessageListener(
        topic = VOUCHER_CACHE_INVALIDATION_TOPIC,
        consumerGroup = VOUCHER_CACHE_INVALIDATION_CONSUMER_GROUP,
        messageModel = MessageModel.BROADCASTING
)
public class VoucherCacheInvalidationConsumer implements RocketMQListener<VoucherCacheInvalidationMessage> {

    @Resource
    private VoucherCacheService voucherCacheService;

    @Override
    public void onMessage(VoucherCacheInvalidationMessage message) {
        if (message == null || message.getShopId() == null) {
            return;
        }
        voucherCacheService.evictAll(message.getShopId());
    }
}
