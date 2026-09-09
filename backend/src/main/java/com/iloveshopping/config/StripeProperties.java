package com.iloveshopping.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Data
@Component
@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {
    /**
     * Active mode: {@code test} (simulated money) or {@code live} (real money moves).
     * Mirrors {@code MPESA_ENVIRONMENT} (sandbox/production) so both rails flip the same way.
     */
    private String environment = "test";

    /** Legacy single keypair — used as fallback when mode-specific keys are blank. */
    private String secretKey;
    private String publishableKey;
    private String webhookSecret;

    private String testSecretKey;
    private String testPublishableKey;
    private String testWebhookSecret;

    private String liveSecretKey;
    private String livePublishableKey;
    private String liveWebhookSecret;

    /** Minimum charge Stripe will accept for the configured currency (in major units). */
    private BigDecimal minAmount = new BigDecimal("100");

    public boolean isLive() {
        return "live".equalsIgnoreCase(environment);
    }

    private static boolean nonBlank(String v) {
        return v != null && !v.isBlank();
    }

    private static boolean startsWithAny(String value, String... prefixes) {
        if (!nonBlank(value)) return false;
        for (String p : prefixes) {
            if (value.startsWith(p)) return true;
        }
        return false;
    }

    /**
     * Mode-safe resolution: a test key must NEVER be used in live mode (the
     * checkout would badge "Live — real money" while charging nothing) and
     * vice versa. Legacy single keys are honored only when their prefix
     * matches the active mode.
     */
    public String effectiveSecretKey() {
        if (isLive()) {
            if (nonBlank(liveSecretKey)) return liveSecretKey;
            return startsWithAny(secretKey, "sk_live_", "rk_live_") ? secretKey : null;
        }
        if (nonBlank(testSecretKey)) return testSecretKey;
        return startsWithAny(secretKey, "sk_test_", "rk_test_") ? secretKey : null;
    }

    public String effectivePublishableKey() {
        if (isLive()) {
            if (nonBlank(livePublishableKey)) return livePublishableKey;
            return startsWithAny(publishableKey, "pk_live_") ? publishableKey : null;
        }
        if (nonBlank(testPublishableKey)) return testPublishableKey;
        return startsWithAny(publishableKey, "pk_test_") ? publishableKey : null;
    }

    public String effectiveWebhookSecret() {
        if (isLive()) {
            return nonBlank(liveWebhookSecret) ? liveWebhookSecret : webhookSecret;
        }
        return nonBlank(testWebhookSecret) ? testWebhookSecret : webhookSecret;
    }
}
