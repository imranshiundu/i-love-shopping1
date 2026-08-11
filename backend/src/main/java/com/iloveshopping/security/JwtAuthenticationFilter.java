package com.iloveshopping.security;

import com.iloveshopping.entity.User;
import com.iloveshopping.repository.SessionRepository;
import com.iloveshopping.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACCESS_TOKEN_COOKIE = "accessToken";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null) {
            try {
                JwtService.JwtClaims claims = jwtService.parseAccessToken(token);

                // Verify session is still valid
                Optional<com.iloveshopping.entity.Session> sessionOpt = sessionRepository.findById(claims.getSessionId());
                if (sessionOpt.isEmpty() || sessionOpt.get().isRevoked()) {
                    log.debug("Session revoked or not found: {}", claims.getSessionId());
                } else {
                    Optional<User> userOpt = userRepository.findById(claims.getSubject());
                    if (userOpt.isPresent() && userOpt.get().isEnabled()) {
                        User user = userOpt.get();
                        List<SimpleGrantedAuthority> authorities = claims.getRoles().stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                .toList();

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                user, null, authorities
                        );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (JwtService.TokenExpiredException e) {
                // Token expired - don't set authentication, let the request continue
                // The controller will handle 401 if needed
                log.debug("Access token expired: {}", e.getMessage());
            } catch (JwtService.InvalidTokenException e) {
                log.debug("Invalid access token: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        // Check Authorization header first
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        // Check cookie
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/v1/auth/register") ||
                path.equals("/api/v1/auth/login") ||
                path.equals("/api/v1/auth/refresh") ||
                path.equals("/api/v1/auth/forgot-password") ||
                path.equals("/api/v1/auth/reset-password") ||
                path.equals("/api/v1/auth/verify-email") ||
                path.equals("/api/v1/auth/verify-captcha") ||
                path.startsWith("/api/v1/products") && request.getMethod().equals("GET") ||
                path.startsWith("/api/v1/categories") && request.getMethod().equals("GET") ||
                path.startsWith("/api/v1/brands") && request.getMethod().equals("GET") ||
                path.equals("/api/v1/health") ||
                path.equals("/api/v1/health/detailed") ||
                path.equals("/api/v1/ready") ||
                path.equals("/api/v1/live") ||
                path.startsWith("/docs") ||
                path.startsWith("/v3/api-docs");
    }
}