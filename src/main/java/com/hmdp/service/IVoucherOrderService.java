package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.OrderTimeoutMessage;
import com.hmdp.dto.Result;
import com.hmdp.dto.VoucherOrderMessage;
import com.hmdp.entity.VoucherOrder;

/**
 * Voucher order service.
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);

    Result queryOrderStatus(Long orderId);

    void createVoucherOrder(VoucherOrderMessage message);

    void handleOrderTimeout(OrderTimeoutMessage message);

    Result mockPay(Long orderId, Boolean success);
}
