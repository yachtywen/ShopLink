package com.hmdp.ratelimit;

public class RateLimitResult {
    private final boolean allowed;
    private final long currentCount;
    private final long retryAfterMillis;

    public RateLimitResult(boolean allowed, long currentCount, long retryAfterMillis) {
        this.allowed = allowed;
        this.currentCount = currentCount;
        this.retryAfterMillis = retryAfterMillis;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public long getCurrentCount() {
        return currentCount;
    }

    public long getRetryAfterMillis() {
        return retryAfterMillis;
    }
}
