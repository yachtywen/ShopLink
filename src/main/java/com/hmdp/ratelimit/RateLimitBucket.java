package com.hmdp.ratelimit;

public class RateLimitBucket {
    private final String key;
    private final int limit;
    private final long windowMillis;

    public RateLimitBucket(String key, int limit, long windowMillis) {
        this.key = key;
        this.limit = limit;
        this.windowMillis = windowMillis;
    }

    public String getKey() {
        return key;
    }

    public int getLimit() {
        return limit;
    }

    public long getWindowMillis() {
        return windowMillis;
    }
}
