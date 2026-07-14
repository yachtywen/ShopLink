package com.hmdp.ratelimit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlidingWindowRateLimiterIntegrationTest {
    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private static SlidingWindowRateLimiter rateLimiter;

    @BeforeAll
    static void setUp() {
        connectionFactory = new LettuceConnectionFactory("127.0.0.1", 6379);
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        rateLimiter = new SlidingWindowRateLimiter(redisTemplate);
    }

    @AfterAll
    static void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void shouldRejectOnlyAfterTheSlidingWindowLimitIsReached() {
        String key = "test:rate:window:" + UUID.randomUUID();
        RateLimitBucket bucket = new RateLimitBucket(key, 2, 1_000L);

        assertTrue(rateLimiter.tryAcquire(Collections.singletonList(bucket)).isAllowed());
        assertTrue(rateLimiter.tryAcquire(Collections.singletonList(bucket)).isAllowed());
        RateLimitResult rejected = rateLimiter.tryAcquire(Collections.singletonList(bucket));

        assertFalse(rejected.isAllowed());
        assertTrue(rejected.getRetryAfterMillis() > 0L);
        redisTemplate.delete(key);
    }

    @Test
    void shouldNotConsumeOtherDimensionsWhenOneDimensionIsAlreadyFull() {
        String blockedKey = "test:rate:blocked:" + UUID.randomUUID();
        String availableKey = "test:rate:available:" + UUID.randomUUID();
        redisTemplate.opsForZSet().add(blockedKey, "existing", System.currentTimeMillis());

        RateLimitResult result = rateLimiter.tryAcquire(Arrays.asList(
                new RateLimitBucket(blockedKey, 1, 10_000L),
                new RateLimitBucket(availableKey, 10, 10_000L)
        ));

        assertFalse(result.isAllowed());
        Long availableCount = redisTemplate.opsForZSet().zCard(availableKey);
        assertEquals(0L, availableCount == null ? 0L : availableCount.longValue());
        redisTemplate.delete(Arrays.asList(blockedKey, availableKey));
    }

    @Test
    void shouldAllowRequestsAgainAfterTheWindowHasElapsed() throws InterruptedException {
        String key = "test:rate:expiry:" + UUID.randomUUID();
        RateLimitBucket bucket = new RateLimitBucket(key, 1, 50L);

        assertTrue(rateLimiter.tryAcquire(Collections.singletonList(bucket)).isAllowed());
        assertFalse(rateLimiter.tryAcquire(Collections.singletonList(bucket)).isAllowed());
        Thread.sleep(80L);
        assertTrue(rateLimiter.tryAcquire(Collections.singletonList(bucket)).isAllowed());
        redisTemplate.delete(key);
    }
}
