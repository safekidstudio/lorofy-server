package com.lorofy.server.core.infrastructure.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation dùng để đánh dấu một Endpoint (Method) hoặc toàn bộ Controller
 * (Class)
 * được phép truy cập ẩn danh (Public / Optional Authentication) giống như
 * NestJS.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface PublicEndpoint {
}
