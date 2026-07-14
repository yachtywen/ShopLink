package com.hmdp.cache;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static com.hmdp.utils.RocketMQConstants.SEND_TIMEOUT_MILLIS;
import static com.hmdp.utils.RocketMQConstants.SHOP_CACHE_INVALIDATION_TOPIC;

/** Clears this instance immediately, then notifies all other instances. */
@Slf4j
@Component
public class ShopCacheInvalidationPublisher {

    @Resource
    private ShopCacheService shopCacheService;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    public void invalidateAndBroadcast(Long shopId) {
        // Always eliminate the fastest stale copy on the writer instance first.
        shopCacheService.evictLocal(shopId);
        try {
            shopCacheService.evictRedis(shopId);
        } catch (Exception e) {
            // The broadcast below gives another instance a chance to remove L2.
            log.error("Failed to evict Redis shop cache, shopId={}", shopId, e);
        }

        try {
            rocketMQTemplate.syncSend(
                    SHOP_CACHE_INVALIDATION_TOPIC,
                    MessageBuilder.withPayload(new ShopCacheInvalidationMessage(shopId)).build(),
                    SEND_TIMEOUT_MILLIS
            );
        } catch (Exception e) {
            // The committed database data is correct. A short L1 TTL bounds stale local data.
            log.error("Failed to broadcast shop cache invalidation, shopId={}", shopId, e);
        }
    }
}
