package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.ratelimit.annotation.LimitRule;
import com.hmdp.ratelimit.annotation.RateLimit;
import com.hmdp.ratelimit.annotation.RateLimitScope;
import com.hmdp.service.IVoucherOrderService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Voucher order APIs.
 */
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @PostMapping("seckill/{id}")
    @RateLimit(key = "voucher-seckill", rules = {
            @LimitRule(scope = RateLimitScope.GLOBAL, limit = 300, windowSeconds = 60),
            @LimitRule(scope = RateLimitScope.IP, limit = 30, windowSeconds = 60),
            @LimitRule(scope = RateLimitScope.USER, limit = 5, windowSeconds = 60)
    })
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.seckillVoucher(voucherId);
    }

    @GetMapping("status/{id}")
    public Result queryOrderStatus(@PathVariable("id") Long orderId) {
        return voucherOrderService.queryOrderStatus(orderId);
    }

    @PostMapping("mock-pay/{id}/{success}")
    @RateLimit(key = "voucher-mock-pay", rules = {
            @LimitRule(scope = RateLimitScope.USER, limit = 10, windowSeconds = 60)
    })
    public Result mockPay(@PathVariable("id") Long orderId, @PathVariable("success") Boolean success) {
        return voucherOrderService.mockPay(orderId, success);
    }
}
