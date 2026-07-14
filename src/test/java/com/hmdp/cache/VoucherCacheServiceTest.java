package com.hmdp.cache;

import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Voucher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.hmdp.utils.RedisConstants.CACHE_VOUCHER_LIST_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_VOUCHER_LIST_TTL;
import static com.hmdp.utils.RedisConstants.SECKILL_STOCK_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherCacheServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private VoucherCacheService cacheService;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheService = new VoucherCacheService(stringRedisTemplate, 100, 60);
    }

    @Test
    void shouldCacheStaticMetadataLocallyAndRefreshStockForEveryResponse() {
        Long shopId = 1L;
        Voucher databaseVoucher = seckillVoucher(10L, shopId, 100);
        AtomicInteger databaseCalls = new AtomicInteger();
        when(valueOperations.get(CACHE_VOUCHER_LIST_KEY + shopId)).thenReturn(null);
        when(valueOperations.multiGet(anyList()))
                .thenReturn(Collections.singletonList("9"))
                .thenReturn(Collections.singletonList("8"));

        List<Voucher> first = cacheService.queryByShopId(shopId, ignored -> {
            databaseCalls.incrementAndGet();
            return Collections.singletonList(databaseVoucher);
        });
        List<Voucher> second = cacheService.queryByShopId(shopId, ignored -> {
            databaseCalls.incrementAndGet();
            return Collections.singletonList(databaseVoucher);
        });

        assertEquals(1, databaseCalls.get());
        assertEquals(9, first.get(0).getStock());
        assertEquals(8, second.get(0).getStock());
        verify(valueOperations, times(2)).multiGet(
                Collections.singletonList(SECKILL_STOCK_KEY + databaseVoucher.getId()));

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.eq(CACHE_VOUCHER_LIST_KEY + shopId),
                jsonCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(CACHE_VOUCHER_LIST_TTL),
                org.mockito.ArgumentMatchers.eq(TimeUnit.MINUTES));
        Voucher cachedVoucher = JSONUtil.parseArray(jsonCaptor.getValue()).toList(Voucher.class).get(0);
        assertNull(cachedVoucher.getStock());
    }

    @Test
    void shouldLoadStaticMetadataFromRedisWithoutCallingDatabase() {
        Long shopId = 2L;
        Voucher staticVoucher = seckillVoucher(20L, shopId, null);
        when(valueOperations.get(CACHE_VOUCHER_LIST_KEY + shopId))
                .thenReturn(JSONUtil.toJsonStr(Collections.singletonList(staticVoucher)));
        when(valueOperations.multiGet(Collections.singletonList(SECKILL_STOCK_KEY + 20L)))
                .thenReturn(Collections.singletonList("7"));
        AtomicInteger databaseCalls = new AtomicInteger();

        List<Voucher> vouchers = cacheService.queryByShopId(shopId, ignored -> {
            databaseCalls.incrementAndGet();
            return Collections.emptyList();
        });

        assertEquals(0, databaseCalls.get());
        assertEquals(7, vouchers.get(0).getStock());
    }

    @Test
    void shouldEvictBothCacheLevels() {
        Long shopId = 3L;
        when(valueOperations.get(CACHE_VOUCHER_LIST_KEY + shopId)).thenReturn("[]");
        cacheService.queryByShopId(shopId, ignored -> Collections.emptyList());

        cacheService.evictAll(shopId);
        cacheService.queryByShopId(shopId, ignored -> Collections.emptyList());

        verify(stringRedisTemplate).delete(CACHE_VOUCHER_LIST_KEY + shopId);
        verify(valueOperations, times(2)).get(CACHE_VOUCHER_LIST_KEY + shopId);
    }

    private Voucher seckillVoucher(Long id, Long shopId, Integer stock) {
        Voucher voucher = new Voucher();
        voucher.setId(id);
        voucher.setShopId(shopId);
        voucher.setTitle("seckill-" + id);
        voucher.setType(1);
        voucher.setStock(stock);
        return voucher;
    }
}
