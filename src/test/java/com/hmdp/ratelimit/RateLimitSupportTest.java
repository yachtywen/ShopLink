package com.hmdp.ratelimit;

import com.hmdp.config.WebExceptionAdvice;
import com.hmdp.dto.Result;
import com.hmdp.ratelimit.annotation.RateLimitScope;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitSupportTest {
    @Test
    void shouldCreateClusterSafeKey() {
        String key = RateLimitKeyFactory.create("voucher-seckill", RateLimitScope.USER, "1");
        assertEquals("rate:sliding:{voucher-seckill}:user:1", key);
    }

    @Test
    void shouldOnlyUseForwardedHeaderWhenExplicitlyTrusted() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.8");
        request.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.1");

        assertEquals("10.0.0.8", new ClientIpResolver(false).resolve(request));
        assertEquals("203.0.113.9", new ClientIpResolver(true).resolve(request));
    }

    @Test
    void shouldReturnResultAnd429ForRateLimitFailures() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        Result result = new WebExceptionAdvice().handleRateLimitException(new RateLimitExceededException(1_500L), response);

        assertEquals(429, response.getStatus());
        assertEquals("2", response.getHeader("Retry-After"));
        assertEquals(false, result.getSuccess());
    }
}
