package com.hmdp.cache;

import com.hmdp.entity.Shop;
import com.hmdp.utils.CacheClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShopCacheServiceTest {

    @Mock
    private CacheClient cacheClient;

    @Mock
    private ShopBloomFilterService bloomFilterService;

    @Test
    void shouldStopBeforeCacheWhenBloomFilterRejectsId() {
        ShopCacheService service = new ShopCacheService(cacheClient, bloomFilterService, 100, 60);
        Function<Long, Shop> dbFallback = ignored -> new Shop();
        when(bloomFilterService.mightContain(999999L)).thenReturn(false);

        assertNull(service.queryById(999999L, dbFallback));

        verify(bloomFilterService).mightContain(999999L);
        verifyNoInteractions(cacheClient);
    }

    @Test
    void shouldUseLocalCacheBeforeRedis() {
        Shop shop = new Shop();
        shop.setId(1L);
        ShopCacheService service = new ShopCacheService(cacheClient, bloomFilterService, 100, 60);
        when(bloomFilterService.mightContain(1L)).thenReturn(true);

        when(cacheClient.queryWithPassThrough(
                ArgumentMatchers.eq(CACHE_SHOP_KEY),
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq(Shop.class),
                ArgumentMatchers.<Function<Long, Shop>>any(),
                ArgumentMatchers.eq(30L),
                ArgumentMatchers.eq(TimeUnit.MINUTES)
        )).thenReturn(shop);

        assertSame(shop, service.queryById(1L, ignored -> null));
        assertSame(shop, service.queryById(1L, ignored -> null));

        verify(cacheClient, times(1)).queryWithPassThrough(
                ArgumentMatchers.eq(CACHE_SHOP_KEY),
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq(Shop.class),
                ArgumentMatchers.<Function<Long, Shop>>any(),
                ArgumentMatchers.eq(30L),
                ArgumentMatchers.eq(TimeUnit.MINUTES)
        );
        verify(bloomFilterService, times(2)).mightContain(1L);
    }

    @Test
    void shouldEvictBothLevels() {
        Shop shop = new Shop();
        shop.setId(2L);
        ShopCacheService service = new ShopCacheService(cacheClient, bloomFilterService, 100, 60);
        when(bloomFilterService.mightContain(2L)).thenReturn(true);
        when(cacheClient.queryWithPassThrough(
                ArgumentMatchers.eq(CACHE_SHOP_KEY),
                ArgumentMatchers.eq(2L),
                ArgumentMatchers.eq(Shop.class),
                ArgumentMatchers.<Function<Long, Shop>>any(),
                ArgumentMatchers.eq(30L),
                ArgumentMatchers.eq(TimeUnit.MINUTES)
        )).thenReturn(shop);

        service.queryById(2L, ignored -> null);
        service.evictAll(2L);
        service.queryById(2L, ignored -> null);

        verify(cacheClient).delete(CACHE_SHOP_KEY + 2L);
        verify(cacheClient, times(2)).queryWithPassThrough(
                ArgumentMatchers.eq(CACHE_SHOP_KEY),
                ArgumentMatchers.eq(2L),
                ArgumentMatchers.eq(Shop.class),
                ArgumentMatchers.<Function<Long, Shop>>any(),
                ArgumentMatchers.eq(30L),
                ArgumentMatchers.eq(TimeUnit.MINUTES)
        );
        verify(bloomFilterService, never()).add(2L);
    }
}
