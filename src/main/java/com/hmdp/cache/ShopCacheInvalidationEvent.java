package com.hmdp.cache;

/**
 * Raised after a shop row has been changed. It is handled after the surrounding
 * database transaction commits, not while the transaction is still open.
 */
public class ShopCacheInvalidationEvent {

    private final Long shopId;

    public ShopCacheInvalidationEvent(Long shopId) {
        this.shopId = shopId;
    }

    public Long getShopId() {
        return shopId;
    }
}
