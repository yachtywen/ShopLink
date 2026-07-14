package com.hmdp.cache;

public class VoucherCacheInvalidationEvent {

    private final Long shopId;

    public VoucherCacheInvalidationEvent(Long shopId) {
        this.shopId = shopId;
    }

    public Long getShopId() {
        return shopId;
    }
}
