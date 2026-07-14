package com.hmdp.ratelimit;

public class RateLimitExceededException extends RuntimeException {
    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterMillis) {
        super("Rate limit exceeded");
        this.retryAfterSeconds = Math.max(1L, (retryAfterMillis + 999L) / 1000L);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
