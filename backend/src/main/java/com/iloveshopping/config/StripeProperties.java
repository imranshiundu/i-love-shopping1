package com.iloveshopping.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Data
@Component
@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {
    private String secretKey;
    private String publishableKey;
    private String webhookSecret;

    /** Minimum charge Stripe will accept for the configured currency (in major units). */
    private BigDecimal minAmount = new BigDecimal("100");
}
