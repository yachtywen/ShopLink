package com.hmdp.cache;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;

import static com.hmdp.utils.RocketMQConstants.SEND_TIMEOUT_MILLIS;
import static com.hmdp.utils.RocketMQConstants.VOUCHER_CACHE_INVALIDATION_TOPIC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VoucherCacheInvalidationTest {

    @Mock
    private VoucherCacheService cacheService;

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @Test
    void shouldEvictWriterCacheAndBroadcastInvalidation() {
        VoucherCacheInvalidationPublisher publisher = new VoucherCacheInvalidationPublisher();
        ReflectionTestUtils.setField(publisher, "voucherCacheService", cacheService);
        ReflectionTestUtils.setField(publisher, "rocketMQTemplate", rocketMQTemplate);
        ArgumentCaptor<Message<?>> messageCaptor = ArgumentCaptor.forClass(Message.class);

        publisher.invalidateAndBroadcast(1L);

        verify(cacheService).evictLocal(1L);
        verify(cacheService).evictRedis(1L);
        verify(rocketMQTemplate).syncSend(
                org.mockito.ArgumentMatchers.eq(VOUCHER_CACHE_INVALIDATION_TOPIC),
                messageCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(SEND_TIMEOUT_MILLIS));
        VoucherCacheInvalidationMessage payload = (VoucherCacheInvalidationMessage) messageCaptor.getValue().getPayload();
        assertEquals(1L, payload.getShopId());
    }

    @Test
    void shouldEvictReceiverCacheWhenBroadcastArrives() {
        VoucherCacheInvalidationConsumer consumer = new VoucherCacheInvalidationConsumer();
        ReflectionTestUtils.setField(consumer, "voucherCacheService", cacheService);

        consumer.onMessage(new VoucherCacheInvalidationMessage(2L));

        verify(cacheService).evictAll(2L);
    }
}
