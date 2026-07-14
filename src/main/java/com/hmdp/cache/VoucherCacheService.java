package com.hmdp.cache;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hmdp.entity.Voucher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.CACHE_VOUCHER_LIST_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_VOUCHER_LIST_TTL;
import static com.hmdp.utils.RedisConstants.SECKILL_STOCK_KEY;

/**
 * Two-level cache for voucher static information. Seckill stock is deliberately
 * excluded and merged from Redis for every response.
 */
@Slf4j
@Component
public class VoucherCacheService {

    private static final int SECKILL_VOUCHER_TYPE = 1;

    private final StringRedisTemplate stringRedisTemplate;
    private final Cache<Long, List<Voucher>> localCache;

    public VoucherCacheService(
            StringRedisTemplate stringRedisTemplate,
            @Value("${hmdp.cache.voucher.local.max-size:10000}") long maxSize,
            @Value("${hmdp.cache.voucher.local.ttl-seconds:60}") long ttlSeconds) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.localCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .build();
    }

    public List<Voucher> queryByShopId(Long shopId, Function<Long, List<Voucher>> dbFallback) {
        List<Voucher> staticVouchers = localCache.getIfPresent(shopId);
        if (staticVouchers == null) {
            staticVouchers = queryRedisOrDatabase(shopId, dbFallback);
            localCache.put(shopId, staticVouchers);
        }
        return mergeLiveStock(staticVouchers);
    }

    public void evictLocal(Long shopId) {
        localCache.invalidate(shopId);
    }

    public void evictRedis(Long shopId) {
        stringRedisTemplate.delete(CACHE_VOUCHER_LIST_KEY + shopId);
    }

    public void evictAll(Long shopId) {
        evictLocal(shopId);
        evictRedis(shopId);
    }

    private List<Voucher> queryRedisOrDatabase(Long shopId, Function<Long, List<Voucher>> dbFallback) {
        String key = CACHE_VOUCHER_LIST_KEY + shopId;
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json != null) {
                return immutableStaticCopy(JSONUtil.parseArray(json).toList(Voucher.class));
            }
        } catch (Exception e) {
            log.error("查询 Redis 优惠券静态缓存失败，将回源数据库，shopId={}", shopId, e);
        }

        List<Voucher> databaseVouchers = dbFallback.apply(shopId);
        List<Voucher> staticVouchers = immutableStaticCopy(databaseVouchers);
        try {
            stringRedisTemplate.opsForValue().set(
                    key, JSONUtil.toJsonStr(staticVouchers), CACHE_VOUCHER_LIST_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("写入 Redis 优惠券静态缓存失败，shopId={}", shopId, e);
        }
        return staticVouchers;
    }

    private List<Voucher> mergeLiveStock(List<Voucher> staticVouchers) {
        List<Voucher> result = mutableCopy(staticVouchers);
        List<Voucher> seckillVouchers = new ArrayList<>();
        List<String> stockKeys = new ArrayList<>();
        for (Voucher voucher : result) {
            if (Integer.valueOf(SECKILL_VOUCHER_TYPE).equals(voucher.getType())) {
                seckillVouchers.add(voucher);
                stockKeys.add(SECKILL_STOCK_KEY + voucher.getId());
            }
        }
        if (stockKeys.isEmpty()) {
            return result;
        }

        try {
            List<String> stocks = stringRedisTemplate.opsForValue().multiGet(stockKeys);
            if (stocks == null) {
                return result;
            }
            for (int i = 0; i < seckillVouchers.size(); i++) {
                String stock = stocks.get(i);
                if (stock != null) {
                    seckillVouchers.get(i).setStock(Integer.valueOf(stock));
                }
            }
        } catch (Exception e) {
            log.error("批量查询 Redis 秒杀库存失败，将返回不含库存的静态信息", e);
        }
        return result;
    }

    private List<Voucher> immutableStaticCopy(List<Voucher> vouchers) {
        if (vouchers == null || vouchers.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(staticCopy(vouchers));
    }

    private List<Voucher> mutableCopy(List<Voucher> vouchers) {
        if (vouchers == null || vouchers.isEmpty()) {
            return new ArrayList<>();
        }
        List<Voucher> copies = new ArrayList<>(vouchers.size());
        for (Voucher voucher : vouchers) {
            Voucher copy = new Voucher();
            BeanUtil.copyProperties(voucher, copy);
            copies.add(copy);
        }
        return copies;
    }

    private List<Voucher> staticCopy(List<Voucher> vouchers) {
        List<Voucher> copies = mutableCopy(vouchers);
        for (Voucher voucher : copies) {
            voucher.setStock(null);
        }
        return copies;
    }
}
