package com.hmdp.config;

import com.hmdp.dto.Result;
import com.hmdp.ratelimit.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletResponse;

@Slf4j
@RestControllerAdvice
public class WebExceptionAdvice {

    @ExceptionHandler(RateLimitExceededException.class)
    public Result handleRateLimitException(RateLimitExceededException e, HttpServletResponse response) {
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(e.getRetryAfterSeconds()));
        return Result.fail("\u64cd\u4f5c\u8fc7\u4e8e\u9891\u7e41\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5");
    }

    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e) {
        log.error(e.toString(), e);
        return Result.fail("服务器异常");
    }
}
