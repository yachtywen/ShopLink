package com.hmdp.cache;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** Payload sent to every application instance to evict its local shop cache. */
@Data
@NoArgsConstructor
public class ShopCacheInvalidationMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long shopId;

    public ShopCacheInvalidationMessage(Long shopId) {
        this.shopId = shopId;
    }
}
