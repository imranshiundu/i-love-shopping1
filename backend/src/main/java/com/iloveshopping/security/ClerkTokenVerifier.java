package com.iloveshopping.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iloveshopping.config.ClerkProperties;
import com.iloveshopping.exception.AuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Verifies Clerk session tokens and resolves the Clerk user profile.
 *
 * <ol>
 *   <li>The session JWT is signature-checked against the instance JWKS
 *       and the issuer must match the JWKS host.</li>
 *   <li>The {@code sub} claim (Clerk user id) is then used to fetch the
 *       verified profile (email, name, avatar) from the Clerk Backend API.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClerkTokenVerifier {

    private final ClerkProperties clerkProperties;
    private final ObjectMapper objectMapper;

    private volatile JwtDecoder jwtDecoder;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public ClerkUser verify(String sessionToken) {
        if (!clerkProperties.isConfigured()) {
            throw AuthenticationException.invalidToken();
        }
        String clerkUserId = verifySignature(sessionToken);
        return fetchUser(clerkUserId);
    }

    private String verifySignature(String sessionToken) {
        try {
            if (sessionToken == null || sessionToken.isBlank()) {
                throw AuthenticationException.invalidToken();
            }
            String[] segments = sessionToken.split("\\.", -1);
            log.warn("Clerk token shape: length={} segments={} prefix='{}'",
                    sessionToken.length(), segments.length,
                    sessionToken.substring(0, Math.min(16, sessionToken.length())));
            JwtDecoder decoder = jwtDecoder;
            if (decoder == null) {
                synchronized (this) {
                    decoder = jwtDecoder;
                    if (decoder == null) {
                        decoder = NimbusJwtDecoder.withJwkSetUri(clerkProperties.getJwksUrl()).build();
                        jwtDecoder = decoder;
                    }
                }
            }
            Jwt jwt = decoder.decode(sessionToken);
            String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : "";
            String expectedIssuer = clerkProperties.getJwksUrl().replace("/.well-known/jwks.json", "");
            if (!expectedIssuer.equals(issuer)) {
                log.warn("Clerk token rejected: unexpected issuer {}", issuer);
                throw AuthenticationException.invalidToken();
            }
            return jwt.getSubject();
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Clerk token verification failed: {}", e.getMessage());
            throw AuthenticationException.invalidToken();
        }
    }

    private ClerkUser fetchUser(String clerkUserId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.clerk.com/v1/users/" + clerkUserId))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + clerkProperties.getSecretKey())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Clerk user lookup failed with status {}", response.statusCode());
                throw AuthenticationException.invalidToken();
            }
            JsonNode root = objectMapper.readTree(response.body());
            String email = primaryEmail(root);
            if (email == null) {
                log.warn("Clerk user {} has no verified email", clerkUserId);
                throw AuthenticationException.invalidToken();
            }
            String firstName = textOrNull(root, "first_name");
            String lastName = textOrNull(root, "last_name");
            String name = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
            return new ClerkUser(clerkUserId, email.toLowerCase(),
                    name.isEmpty() ? null : name, textOrNull(root, "image_url"));
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Clerk user lookup failed: {}", e.getMessage());
            throw AuthenticationException.invalidToken();
        }
    }

    private String primaryEmail(JsonNode root) {
        String primaryId = textOrNull(root, "primary_email_address_id");
        JsonNode addresses = root.get("email_addresses");
        if (addresses == null || !addresses.isArray()) {
            return null;
        }
        String fallback = null;
        for (JsonNode address : addresses) {
            String verification = address.path("verification").path("status").asText("");
            if (!"verified".equals(verification)) {
                continue;
            }
            String email = textOrNull(address, "email_address");
            if (email == null) {
                continue;
            }
            if (address.path("id").asText("").equals(primaryId)) {
                return email;
            }
            if (fallback == null) {
                fallback = email;
            }
        }
        return fallback;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value == null || value.isNull() || value.asText("").isBlank()) ? null : value.asText();
    }

    public record ClerkUser(String id, String email, String name, String avatarUrl) {
    }
}
