package com.lorofy.server.core.infrastructure.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    /**
     * Header name to look for the idempotency key (default: X-Idempotency-Key)
     */
    String headerName() default "X-Idempotency-Key";

    /**
     * Expiration time in seconds for the cached response in Redis (default: 300 seconds / 5 mins)
     */
    long ttlSeconds() default 300;
}
