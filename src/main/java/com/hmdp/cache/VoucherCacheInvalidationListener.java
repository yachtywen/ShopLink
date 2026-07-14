package com.hmdp.cache;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.annotation.Resource;

@Component
public class VoucherCacheInvalidationListener {

    @Resource
    private VoucherCacheInvalidationPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVoucherChanged(VoucherCacheInvalidationEvent event) {
        publisher.invalidateAndBroadcast(event.getShopId());
    }
}
