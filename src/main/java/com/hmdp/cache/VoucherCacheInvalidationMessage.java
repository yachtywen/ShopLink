package com.hmdp.cache;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class VoucherCacheInvalidationMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long shopId;

    public VoucherCacheInvalidationMessage(Long shopId) {
        this.shopId = shopId;
    }
}
