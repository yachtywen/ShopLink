package com.hmdp.cache;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShopBloomFilterServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private ShopMapper shopMapper;

    @Mock
    private RBloomFilter<Long> bloomFilter;

    @Test
    void shouldLoadExistingShopIdsAndCheckMembership() {
        ShopBloomFilterService service = service();
        when(redissonClient.<Long>getBloomFilter("test:shop:bloom")).thenReturn(bloomFilter);
        when(bloomFilter.tryInit(100L, 0.01D)).thenReturn(true);
        when(shopMapper.selectObjs(any(Wrapper.class))).thenReturn(Arrays.<Object>asList(1L, 2L));
        when(bloomFilter.contains(1L)).thenReturn(true);
        when(bloomFilter.contains(999L)).thenReturn(false);

        service.initialize();

        verify(bloomFilter).add(1L);
        verify(bloomFilter).add(2L);
        assertTrue(service.mightContain(1L));
        assertFalse(service.mightContain(999L));
    }

    @Test
    void shouldFailOpenWhenFilterIsNotReady() {
        ShopBloomFilterService service = service();

        assertTrue(service.mightContain(1L));

        verify(redissonClient, never()).getBloomFilter("test:shop:bloom");
    }

    @Test
    void shouldFailOpenWhenMembershipCheckThrows() {
        ShopBloomFilterService service = initializedService();
        when(bloomFilter.contains(1L)).thenThrow(new IllegalStateException("redis unavailable"));

        assertTrue(service.mightContain(1L));
    }

    @Test
    void shouldDisableFilteringWhenAddingNewShopFails() {
        ShopBloomFilterService service = initializedService();
        when(bloomFilter.add(3L)).thenThrow(new IllegalStateException("redis unavailable"));

        service.add(3L);

        assertTrue(service.mightContain(3L));
        verify(bloomFilter, never()).contains(3L);
    }

    private ShopBloomFilterService initializedService() {
        ShopBloomFilterService service = service();
        when(redissonClient.<Long>getBloomFilter("test:shop:bloom")).thenReturn(bloomFilter);
        when(bloomFilter.tryInit(100L, 0.01D)).thenReturn(true);
        when(shopMapper.selectObjs(any(Wrapper.class))).thenReturn(Arrays.<Object>asList(1L, 2L));
        service.initialize();
        return service;
    }

    private ShopBloomFilterService service() {
        return new ShopBloomFilterService(redissonClient, shopMapper, "test:shop:bloom", 100L, 0.01D);
    }
}
