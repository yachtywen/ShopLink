package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.cache.VoucherCacheInvalidationEvent;
import com.hmdp.cache.VoucherCacheService;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

import static com.hmdp.utils.RedisConstants.SECKILL_STOCK_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    private static final int SECKILL_VOUCHER_TYPE = 1;

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private VoucherCacheService voucherCacheService;
    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        List<Voucher> vouchers = voucherCacheService.queryByShopId(
                shopId, getBaseMapper()::queryVoucherOfShop);
        return Result.ok(vouchers);
    }

    @Override
    @Transactional
    public boolean save(Voucher voucher) {
        boolean saved = super.save(voucher);
        if (saved && voucher.getShopId() != null) {
            applicationEventPublisher.publishEvent(new VoucherCacheInvalidationEvent(voucher.getShopId()));
        }
        return saved;
    }

    @Override
    @Transactional
    public boolean updateById(Voucher voucher) {
        Long shopId = voucher.getShopId();
        if (shopId == null && voucher.getId() != null) {
            Voucher existing = getById(voucher.getId());
            shopId = existing == null ? null : existing.getShopId();
        }
        boolean updated = super.updateById(voucher);
        if (updated && shopId != null) {
            applicationEventPublisher.publishEvent(new VoucherCacheInvalidationEvent(shopId));
        }
        return updated;
    }

    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        voucher.setType(SECKILL_VOUCHER_TYPE);
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);
        // 保存秒杀库存到Redis中
        stringRedisTemplate.opsForValue().set(SECKILL_STOCK_KEY + voucher.getId(), voucher.getStock().toString());
    }
}
