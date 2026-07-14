package com.hmdp.service.impl;

import com.hmdp.cache.ShopBloomFilterService;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShopServiceImplTest {

    @Mock
    private ShopMapper shopMapper;

    @Mock
    private ShopBloomFilterService bloomFilterService;

    private ShopServiceImpl shopService;

    @BeforeEach
    void setUp() {
        shopService = new ShopServiceImpl();
        ReflectionTestUtils.setField(shopService, "baseMapper", shopMapper);
        ReflectionTestUtils.setField(shopService, "shopBloomFilterService", bloomFilterService);
    }

    @Test
    void shouldAddShopIdToBloomFilterAfterDatabaseInsertSucceeds() {
        Shop shop = new Shop();
        shop.setId(101L);
        when(shopMapper.insert(shop)).thenReturn(1);

        assertTrue(shopService.save(shop));

        verify(bloomFilterService).add(101L);
    }

    @Test
    void shouldNotAddShopIdWhenDatabaseInsertFails() {
        Shop shop = new Shop();
        shop.setId(102L);
        when(shopMapper.insert(shop)).thenReturn(0);

        assertFalse(shopService.save(shop));

        verify(bloomFilterService, never()).add(102L);
    }
}
