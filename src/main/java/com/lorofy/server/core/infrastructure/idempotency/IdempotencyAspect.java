package com.lorofy.server.core.infrastructure.idempotency;

import java.time.Duration;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyAspect {

    private final RedisTemplate<String, Object> redisTemplate;

    @Around("@annotation(idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String idempotencyKey = request.getHeader(idempotent.headerName());

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return joinPoint.proceed();
        }

        String redisKey = "idempotency:" + joinPoint.getSignature().toShortString() + ":" + idempotencyKey;

        // Check if response already exists in Redis
        Object cachedResponse = redisTemplate.opsForValue().get(redisKey);
        if (cachedResponse != null) {
            log.info("Idempotent request intercepted for key: {}. Returning cached response.", redisKey);
            return cachedResponse;
        }

        // Atomically acquire lock for processing
        String lockKey = redisKey + ":lock";
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "IN_PROGRESS", Duration.ofSeconds(30));
        if (Boolean.FALSE.equals(acquired)) {
            throw new IllegalStateException("A request with the same Idempotency-Key is currently being processed.");
        }

        try {
            Object result = joinPoint.proceed();
            if (result != null) {
                redisTemplate.opsForValue().set(redisKey, result, Duration.ofSeconds(idempotent.ttlSeconds()));
            }
            return result;
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
}
