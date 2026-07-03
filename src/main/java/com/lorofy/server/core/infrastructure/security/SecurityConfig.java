package com.lorofy.server.core.infrastructure.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    @Lazy
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    // Bean for Password Encoder (BCrypt)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Bean for Authentication Manager
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 1. Quét động các Controller để lấy danh sách API public
        List<RequestMatcher> publicMatchers = getPublicEndpointsFromControllers();

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 2. Nạp danh sách quét động này vào để permitAll()
                        .requestMatchers(publicMatchers.toArray(new RequestMatcher[0])).permitAll()

                        // Các URL hạ tầng công khai tĩnh
                        .requestMatchers("/api/v1/health").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/api-docs", "/api-docs/**",
                                "/api-docs-json", "/api-docs-json/**")
                        .permitAll()

                        // Các request khác bắt buộc phải đăng nhập
                        .anyRequest().authenticated());

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private List<RequestMatcher> getPublicEndpointsFromControllers() {
        List<RequestMatcher> matchers = new ArrayList<>();

        // Lấy tất cả các Mapping thông tin trong dự án
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();

            // Kiểm tra xem Annotation @PublicEndpoint có xuất hiện trên Method hoặc Class
            // không
            if (handlerMethod.hasMethodAnnotation(PublicEndpoint.class) ||
                    handlerMethod.getBeanType().isAnnotationPresent(PublicEndpoint.class)) {

                Set<String> patterns = mappingInfo.getPatternValues();
                Set<RequestMethod> methods = mappingInfo.getMethodsCondition().getMethods();

                // Tạo PathPatternRequestMatcher tương ứng cho từng URL và HTTP Method (Spring
                // Security 7)
                for (String pattern : patterns) {
                    if (methods.isEmpty()) {
                        // Nếu không giới hạn method -> Cho phép mọi HTTP method đi qua URL này
                        matchers.add(PathPatternRequestMatcher.pathPattern(pattern));
                    } else {
                        for (RequestMethod method : methods) {
                            // Map RequestMethod của Spring Web sang HttpMethod của Spring Security
                            HttpMethod httpMethod = HttpMethod.valueOf(method.name());
                            matchers.add(PathPatternRequestMatcher.pathPattern(httpMethod, pattern));
                        }
                    }
                }
            }
        }
        return matchers;
    }

}