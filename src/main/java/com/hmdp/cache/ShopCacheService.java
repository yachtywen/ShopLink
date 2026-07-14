package com.hmdp.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hmdp.entity.Shop;
import com.hmdp.utils.CacheClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;

/**
 * Shop-detail read path: Bloom filter, Caffeine L1, Redis L2, then MySQL.
 * Only non-null shop records enter L1; Redis retains the null-value marker for
 * Bloom-filter false positives and deleted shops.
 */
@Component
public class ShopCacheService {

    private final CacheClient cacheClient;
    private final ShopBloomFilterService bloomFilterService;
    private final Cache<Long, Shop> localCache;

    public ShopCacheService(
            CacheClient cacheClient,
            ShopBloomFilterService bloomFilterService,
            @Value("${hmdp.cache.shop.local.max-size:10000}") long maxSize,
            @Value("${hmdp.cache.shop.local.ttl-seconds:60}") long ttlSeconds) {
        this.cacheClient = cacheClient;
        this.bloomFilterService = bloomFilterService;
        this.localCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .build();
    }

    public Shop queryById(Long id, Function<Long, Shop> dbFallback) {
        if (!bloomFilterService.mightContain(id)) {
            return null;
        }
        Shop localShop = localCache.getIfPresent(id);
        if (localShop != null) {
            return localShop;
        }
        Shop shop = cacheClient.queryWithPassThrough(
                CACHE_SHOP_KEY, id, Shop.class, dbFallback, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if (shop != null) {
            localCache.put(id, shop);
        }
        return shop;
    }

    public void evictLocal(Long shopId) {
        localCache.invalidate(shopId);
    }

    public void evictRedis(Long shopId) {
        cacheClient.delete(CACHE_SHOP_KEY + shopId);
    }

    public void evictAll(Long shopId) {
        evictLocal(shopId);
        evictRedis(shopId);
    }
}
