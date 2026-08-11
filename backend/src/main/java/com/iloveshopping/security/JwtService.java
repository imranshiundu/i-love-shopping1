package com.iloveshopping.security;

import com.iloveshopping.config.JwtProperties;
import com.iloveshopping.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private final JwtProperties jwtProperties;
    private SecretKey accessKey;
    private SecretKey refreshKey;

    @PostConstruct
    public void init() {
        this.accessKey = Keys.hmacShaKeyFor(jwtProperties.getAccessSecret().getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(jwtProperties.getRefreshSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user, String sessionId) {
        Instant now = Instant.now();
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(auth -> auth.replace("ROLE_", ""))
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(user.getId())
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .claim("roles", roles)
                .claim("sessionId", sessionId)
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(jwtProperties.getAccessExpiryMillis())))
                .id(UUID.randomUUID().toString())
                .signWith(accessKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(User user, String sessionId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId())
                .claim("sessionId", sessionId)
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(jwtProperties.getRefreshExpiryMillis())))
                .id(UUID.randomUUID().toString())
                .signWith(refreshKey, Jwts.SIG.HS256)
                .compact();
    }

    public JwtClaims parseAccessToken(String token) {
        return parseToken(token, accessKey);
    }

    public JwtClaims parseRefreshToken(String token) {
        return parseToken(token, refreshKey);
    }

    private JwtClaims parseToken(String token, SecretKey key) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(jwtProperties.getIssuer())
                    .requireAudience(jwtProperties.getAudience())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return JwtClaims.builder()
                    .subject(claims.getSubject())
                    .email(claims.get("email", String.class))
                    .name(claims.get("name", String.class))
                    .roles(claims.get("roles", List.class))
                    .sessionId(claims.get("sessionId", String.class))
                    .issuedAt(claims.getIssuedAt().toInstant())
                    .expiresAt(claims.getExpiration().toInstant())
                    .tokenId(claims.getId())
                    .build();
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException("Token has expired");
        } catch (JwtException e) {
            log.debug("Invalid token: {}", e.getMessage());
            throw new InvalidTokenException("Invalid token: " + e.getMessage());
        }
    }

    public boolean validateAccessToken(String token) {
        try {
            parseAccessToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            parseRefreshToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static class TokenExpiredException extends RuntimeException {
        public TokenExpiredException(String message) {
            super(message);
        }
    }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }

    @lombok.Value
    @lombok.Builder
    public static class JwtClaims {
        String subject;
        String email;
        String name;
        List<String> roles;
        String sessionId;
        Instant issuedAt;
        Instant expiresAt;
        String tokenId;
    }
}