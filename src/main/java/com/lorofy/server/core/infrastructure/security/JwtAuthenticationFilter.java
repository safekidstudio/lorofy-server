package com.lorofy.server.core.infrastructure.security;

import java.io.IOException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.lorofy.server.core.infrastructure.redis.RedisKeyBuilder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Get token from header
            String jwt = getJwtFromRequest(request);

            // Validate token
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // Check if the token is in the blacklist
                String blacklistKey = RedisKeyBuilder.getJwtBlacklistKey(jwt);
                Boolean isBlacklisted = redisTemplate.hasKey(blacklistKey);
                if (Boolean.TRUE.equals(isBlacklisted)) {
                    log.warn("Unauthorized request: JWT token is blacklisted");
                    filterChain.doFilter(request, response);
                    return;
                }
                UserPrincipal userPrincipal = tokenProvider.getUserPrincipalFromJWT(jwt);
                // Create authentication in Spring Security
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userPrincipal, null, userPrincipal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // Set authentication in SecurityContextHolder, so that it is accessible to
                // other parts of the application
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }
        // Continue the filter chain
        filterChain.doFilter(request, response);
    }

    // Helper method to extract JWT token from HTTP request
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

}
