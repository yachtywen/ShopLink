package com.hmdp.cache;

import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static com.hmdp.utils.RocketMQConstants.SHOP_CACHE_INVALIDATION_CONSUMER_GROUP;
import static com.hmdp.utils.RocketMQConstants.SHOP_CACHE_INVALIDATION_TOPIC;

/**
 * BROADCASTING is required: every application instance owns a separate Caffeine
 * cache and must receive the invalidation message.
 */
@Component
@RocketMQMessageListener(
        topic = SHOP_CACHE_INVALIDATION_TOPIC,
        consumerGroup = SHOP_CACHE_INVALIDATION_CONSUMER_GROUP,
        messageModel = MessageModel.BROADCASTING
)
public class ShopCacheInvalidationConsumer implements RocketMQListener<ShopCacheInvalidationMessage> {

    @Resource
    private ShopCacheService shopCacheService;

    @Override
    public void onMessage(ShopCacheInvalidationMessage message) {
        if (message == null || message.getShopId() == null) {
            return;
        }
        shopCacheService.evictAll(message.getShopId());
    }
}
