package com.lorofy.server.core.security;

import java.io.IOException;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

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
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Get token from header
            String jwt = getJwtFromRequest(request);

            // Validate token
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // Get userId from Token
                UUID userId = tokenProvider.getUserIdFromJWT(jwt);
                // Get UserDetails from userId
                UserDetails userDetails = customUserDetailsService.loadUserById(userId);
                // Create authentication in Spring Security
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
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
