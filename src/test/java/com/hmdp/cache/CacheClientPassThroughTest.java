package com.hmdp.cache;

import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import com.hmdp.utils.CacheClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.CACHE_NULL_TTL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CacheClientPassThroughTest {

    private static final String KEY_PREFIX = "cache:shop:";

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private CacheClient cacheClient;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheClient = new CacheClient(stringRedisTemplate);
    }

    @Test
    void shouldReturnRedisValueWithoutCallingDatabase() {
        Shop cached = shop(1L);
        AtomicBoolean databaseCalled = new AtomicBoolean(false);
        when(valueOperations.get(KEY_PREFIX + 1L)).thenReturn(JSONUtil.toJsonStr(cached));

        Shop result = cacheClient.queryWithPassThrough(
                KEY_PREFIX, 1L, Shop.class, databaseFallback(databaseCalled, null), 30L, TimeUnit.MINUTES);

        assertEquals(cached, result);
        assertFalse(databaseCalled.get());
        verify(valueOperations, never()).set(KEY_PREFIX + 1L, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
    }

    @Test
    void shouldReturnNullForCachedEmptyValueWithoutCallingDatabase() {
        AtomicBoolean databaseCalled = new AtomicBoolean(false);
        when(valueOperations.get(KEY_PREFIX + 2L)).thenReturn("");

        Shop result = cacheClient.queryWithPassThrough(
                KEY_PREFIX, 2L, Shop.class, databaseFallback(databaseCalled, null), 30L, TimeUnit.MINUTES);

        assertNull(result);
        assertFalse(databaseCalled.get());
    }

    @Test
    void shouldLoadDatabaseValueAndPopulateRedis() {
        Shop databaseShop = shop(3L);
        AtomicBoolean databaseCalled = new AtomicBoolean(false);
        when(valueOperations.get(KEY_PREFIX + 3L)).thenReturn(null);

        Shop result = cacheClient.queryWithPassThrough(
                KEY_PREFIX, 3L, Shop.class, databaseFallback(databaseCalled, databaseShop), 30L, TimeUnit.MINUTES);

        assertEquals(databaseShop, result);
        assertTrue(databaseCalled.get());
        verify(valueOperations).set(KEY_PREFIX + 3L, JSONUtil.toJsonStr(databaseShop), 30L, TimeUnit.MINUTES);
    }

    @Test
    void shouldCacheEmptyValueWhenDatabaseDoesNotContainShop() {
        AtomicBoolean databaseCalled = new AtomicBoolean(false);
        when(valueOperations.get(KEY_PREFIX + 4L)).thenReturn(null);

        Shop result = cacheClient.queryWithPassThrough(
                KEY_PREFIX, 4L, Shop.class, databaseFallback(databaseCalled, null), 30L, TimeUnit.MINUTES);

        assertNull(result);
        assertTrue(databaseCalled.get());
        verify(valueOperations).set(KEY_PREFIX + 4L, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
    }

    private Function<Long, Shop> databaseFallback(AtomicBoolean called, Shop result) {
        return ignored -> {
            called.set(true);
            return result;
        };
    }

    private Shop shop(Long id) {
        Shop shop = new Shop();
        shop.setId(id);
        shop.setName("shop-" + id);
        return shop;
    }
}
