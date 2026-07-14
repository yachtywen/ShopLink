package com.hmdp.ratelimit;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class SlidingWindowRateLimiter {
    private static final DefaultRedisScript<List> SLIDING_WINDOW_SCRIPT;

    static {
        SLIDING_WINDOW_SCRIPT = new DefaultRedisScript<List>();
        SLIDING_WINDOW_SCRIPT.setLocation(new ClassPathResource("rate_limit_sliding_window.lua"));
        SLIDING_WINDOW_SCRIPT.setResultType(List.class);
    }

    private final StringRedisTemplate stringRedisTemplate;

    public SlidingWindowRateLimiter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public RateLimitResult tryAcquire(List<RateLimitBucket> buckets) {
        if (buckets == null || buckets.isEmpty()) {
            return new RateLimitResult(true, 0L, 0L);
        }
        List<String> keys = new ArrayList<String>(buckets.size());
        List<String> args = new ArrayList<String>(2 + buckets.size() * 2);
        args.add(String.valueOf(System.currentTimeMillis()));
        args.add(UUID.randomUUID().toString());
        for (RateLimitBucket bucket : buckets) {
            keys.add(bucket.getKey());
            args.add(String.valueOf(bucket.getLimit()));
            args.add(String.valueOf(bucket.getWindowMillis()));
        }
        List result = stringRedisTemplate.execute(SLIDING_WINDOW_SCRIPT, keys, args.toArray());
        if (result == null || result.size() < 3) {
            throw new IllegalStateException("Rate limit Lua script returned an invalid result");
        }
        boolean allowed = asLong(result.get(0)) == 1L;
        return new RateLimitResult(allowed, asLong(result.get(1)), asLong(result.get(2)));
    }

    private long asLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
