package com.hmdp.cache;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static com.hmdp.utils.RocketMQConstants.SEND_TIMEOUT_MILLIS;
import static com.hmdp.utils.RocketMQConstants.VOUCHER_CACHE_INVALIDATION_TOPIC;

@Slf4j
@Component
public class VoucherCacheInvalidationPublisher {

    @Resource
    private VoucherCacheService voucherCacheService;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    public void invalidateAndBroadcast(Long shopId) {
        voucherCacheService.evictLocal(shopId);
        try {
            voucherCacheService.evictRedis(shopId);
        } catch (Exception e) {
            log.error("删除 Redis 优惠券静态缓存失败，shopId={}", shopId, e);
        }

        try {
            rocketMQTemplate.syncSend(
                    VOUCHER_CACHE_INVALIDATION_TOPIC,
                    MessageBuilder.withPayload(new VoucherCacheInvalidationMessage(shopId)).build(),
                    SEND_TIMEOUT_MILLIS
            );
        } catch (Exception e) {
            log.error("广播优惠券缓存失效消息失败，shopId={}", shopId, e);
        }
    }
}
