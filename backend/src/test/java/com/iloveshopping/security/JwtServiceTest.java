package com.iloveshopping.security;

import com.iloveshopping.config.JwtProperties;
import com.iloveshopping.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;
    private JwtProperties jwtProperties;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        // Use proper 32+ character secrets for HS256 (256-bit key)
        jwtProperties.setAccessSecret("test-access-secret-key-32-chars-long-1234567890");
        jwtProperties.setRefreshSecret("test-refresh-secret-key-32-chars-long-1234567890");
        jwtProperties.setAccessExpiryMinutes(15);
        jwtProperties.setRefreshExpiryDays(7);
        jwtProperties.setIssuer("i-love-shopping");
        jwtProperties.setAudience("i-love-shopping-api");

        jwtService = new JwtService(jwtProperties);
        jwtService.init();

        testUser = User.builder()
                .id("test-user-id")
                .email("test@example.com")
                .name("Test User")
                .passwordHash("hashed-password")
                .emailVerified(LocalDateTime.now())
                .twoFactorEnabled(false)
                .roles(Set.of(User.Role.USER))
                .build();
    }

    @Test
    void shouldGenerateValidAccessToken() {
        String token = jwtService.generateAccessToken(testUser, "session-123");
        assertNotNull(token);

        JwtService.JwtClaims claims = jwtService.parseAccessToken(token);
        assertEquals("test-user-id", claims.getSubject());
        assertEquals("test@example.com", claims.getEmail());
        assertEquals("Test User", claims.getName());
        assertEquals("session-123", claims.getSessionId());
        assertTrue(claims.getRoles().contains("USER"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiresAt());
        assertNotNull(claims.getTokenId());
    }

    @Test
    void shouldGenerateValidRefreshToken() {
        String token = jwtService.generateRefreshToken(testUser);
        assertNotNull(token);

        JwtService.JwtClaims claims = jwtService.parseRefreshToken(token);
        assertEquals("test-user-id", claims.getSubject());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiresAt());
        assertNotNull(claims.getTokenId());
    }

    @Test
    void shouldThrowOnInvalidAccessToken() {
        assertThrows(JwtService.InvalidTokenException.class, () -> {
            jwtService.parseAccessToken("invalid-token");
        });
    }

    @Test
    void shouldThrowOnInvalidRefreshToken() {
        assertThrows(JwtService.InvalidTokenException.class, () -> {
            jwtService.parseRefreshToken("invalid-token");
        });
    }

    @Test
    void shouldValidateCorrectAccessToken() {
        String token = jwtService.generateAccessToken(testUser, "session-123");
        assertTrue(jwtService.validateAccessToken(token));
    }

    @Test
    void shouldInvalidateCorrectRefreshToken() {
        String token = jwtService.generateRefreshToken(testUser);
        assertTrue(jwtService.validateRefreshToken(token));
    }

    @Test
    void shouldNotValidateInvalidToken() {
        assertFalse(jwtService.validateAccessToken("invalid-token"));
        assertFalse(jwtService.validateRefreshToken("invalid-token"));
    }

    @Test
    void shouldIncludeRolesInToken() {
        User adminUser = User.builder()
                .id("admin-id")
                .email("admin@example.com")
                .passwordHash("hash")
                .roles(Set.of(User.Role.USER, User.Role.ADMIN))
                .build();

        String token = jwtService.generateAccessToken(adminUser, "session-456");
        JwtService.JwtClaims claims = jwtService.parseAccessToken(token);

        assertTrue(claims.getRoles().contains("USER"));
        assertTrue(claims.getRoles().contains("ADMIN"));
    }

    @Test
    void shouldExpireAccessTokensWithinConfiguredTime() {
        jwtProperties.setAccessExpiryMinutes(1);
        JwtService service = new JwtService(jwtProperties);
        service.init();

        String token = service.generateAccessToken(testUser, "session-123");
        JwtService.JwtClaims claims = service.parseAccessToken(token);

        assertNotNull(claims.getExpiresAt());
        assertTrue(claims.getExpiresAt().isAfter(claims.getIssuedAt()));
        assertTrue(claims.getExpiresAt().isBefore(claims.getIssuedAt().plusSeconds(120)));
    }
}