package com.hmdp.cache;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Bloom filter for existing shop IDs. A negative result can safely short-circuit
 * the cache lookup; failures always degrade to "might exist" to avoid false
 * negatives caused by infrastructure problems.
 */
@Slf4j
@Component
public class ShopBloomFilterService {

    private final RedissonClient redissonClient;
    private final ShopMapper shopMapper;
    private final String filterName;
    private final long expectedInsertions;
    private final double falseProbability;

    private volatile RBloomFilter<Long> bloomFilter;
    private volatile boolean ready;

    public ShopBloomFilterService(
            RedissonClient redissonClient,
            ShopMapper shopMapper,
            @Value("${hmdp.cache.shop.bloom.name:cache:bloom:shop:v1}") String filterName,
            @Value("${hmdp.cache.shop.bloom.expected-insertions:100000}") long expectedInsertions,
            @Value("${hmdp.cache.shop.bloom.false-probability:0.01}") double falseProbability) {
        this.redissonClient = redissonClient;
        this.shopMapper = shopMapper;
        this.filterName = filterName;
        this.expectedInsertions = expectedInsertions;
        this.falseProbability = falseProbability;
    }

    @PostConstruct
    public void initialize() {
        ready = false;
        try {
            RBloomFilter<Long> filter = redissonClient.getBloomFilter(filterName);
            boolean initialized = filter.tryInit(expectedInsertions, falseProbability);
            List<Object> shopIds = shopMapper.selectObjs(new QueryWrapper<Shop>().select("id"));
            for (Object shopId : shopIds) {
                if (shopId != null) {
                    filter.add(Long.valueOf(String.valueOf(shopId)));
                }
            }
            bloomFilter = filter;
            ready = true;
            log.info("店铺布隆过滤器校准完成，name={}, initialized={}, shopCount={}",
                    filterName, initialized, shopIds.size());
        } catch (Exception e) {
            log.error("店铺布隆过滤器初始化失败，将降级为原有缓存查询链路，name={}", filterName, e);
        }
    }

    public boolean mightContain(Long shopId) {
        if (shopId == null) {
            return false;
        }
        RBloomFilter<Long> filter = bloomFilter;
        if (!ready || filter == null) {
            return true;
        }
        try {
            return filter.contains(shopId);
        } catch (Exception e) {
            log.error("查询店铺布隆过滤器失败，将降级放行，shopId={}", shopId, e);
            return true;
        }
    }

    public void add(Long shopId) {
        if (shopId == null) {
            return;
        }
        RBloomFilter<Long> filter = bloomFilter;
        if (!ready || filter == null) {
            log.warn("店铺布隆过滤器尚未就绪，跳过新增店铺 ID，shopId={}", shopId);
            return;
        }
        try {
            filter.add(shopId);
        } catch (Exception e) {
            ready = false;
            log.error("新增店铺 ID 写入布隆过滤器失败，将禁用当前实例的布隆过滤，shopId={}", shopId, e);
        }
    }
}
