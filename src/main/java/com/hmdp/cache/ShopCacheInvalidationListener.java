package com.hmdp.cache;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.annotation.Resource;

/** Runs only after the database transaction is committed successfully. */
@Component
public class ShopCacheInvalidationListener {

    @Resource
    private ShopCacheInvalidationPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onShopUpdated(ShopCacheInvalidationEvent event) {
        publisher.invalidateAndBroadcast(event.getShopId());
    }
}
