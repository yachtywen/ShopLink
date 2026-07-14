package com.hmdp.ratelimit;

import com.hmdp.dto.UserDTO;
import com.hmdp.ratelimit.annotation.LimitRule;
import com.hmdp.ratelimit.annotation.RateLimit;
import com.hmdp.ratelimit.annotation.RateLimitScope;
import com.hmdp.utils.UserHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Aspect
@Component
@Order(0)
public class RateLimitAspect {
    private final SlidingWindowRateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;

    public RateLimitAspect(SlidingWindowRateLimiter rateLimiter, ClientIpResolver clientIpResolver) {
        this.rateLimiter = rateLimiter;
        this.clientIpResolver = clientIpResolver;
    }

    @Around("@annotation(rateLimit)")
    public Object enforce(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }
        HttpServletRequest request = attributes.getRequest();
        List<RateLimitBucket> buckets = createBuckets(rateLimit, request);
        RateLimitResult result = rateLimiter.tryAcquire(buckets);
        if (!result.isAllowed()) {
            throw new RateLimitExceededException(result.getRetryAfterMillis());
        }
        return joinPoint.proceed();
    }

    private List<RateLimitBucket> createBuckets(RateLimit rateLimit, HttpServletRequest request) {
        List<RateLimitBucket> buckets = new ArrayList<RateLimitBucket>();
        UserDTO user = UserHolder.getUser();
        for (LimitRule rule : rateLimit.rules()) {
            if (rule.limit() <= 0 || rule.windowSeconds() <= 0) {
                throw new IllegalArgumentException("Rate limit and windowSeconds must be positive");
            }
            String identifier = identifierFor(rule.scope(), request, user);
            if (identifier == null) {
                continue;
            }
            String key = RateLimitKeyFactory.create(rateLimit.key(), rule.scope(), identifier);
            buckets.add(new RateLimitBucket(key, rule.limit(), rule.windowSeconds() * 1000L));
        }
        return buckets;
    }

    private String identifierFor(RateLimitScope scope, HttpServletRequest request, UserDTO user) {
        if (scope == RateLimitScope.GLOBAL) {
            return "all";
        }
        if (scope == RateLimitScope.IP) {
            return clientIpResolver.resolve(request);
        }
        return user == null ? null : String.valueOf(user.getId());
    }
}
