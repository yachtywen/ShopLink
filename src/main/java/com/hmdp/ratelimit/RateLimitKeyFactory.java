package com.hmdp.ratelimit;

import com.hmdp.ratelimit.annotation.RateLimitScope;

public final class RateLimitKeyFactory {
    private RateLimitKeyFactory() {
    }

    public static String create(String resource, RateLimitScope scope, String identifier) {
        // Hash tags keep all dimensions of one annotated resource in the same slot in Redis Cluster.
        return "rate:sliding:{" + resource + "}:" + scope.name().toLowerCase() + ":" + identifier;
    }
}
