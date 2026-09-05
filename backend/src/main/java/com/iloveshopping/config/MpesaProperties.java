package com.iloveshopping.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mpesa")
public class MpesaProperties {
    private String environment = "sandbox";
    private String consumerKey;
    private String consumerSecret;
    private String shortcode;
    private String passkey;
    private String callbackUrl;
    private String timeoutUrl;
    private String baseUrl = "https://sandbox.safaricom.co.ke";
    /**
     * Seconds an STK push prompt stays payable. Safaricom expires the phone
     * prompt after ~60-120s; when no success callback arrives within this
     * window the payment is auto-marked FAILED and the customer gets a
     * payable invoice email instead of starting over.
     */
    private int stkTimeoutSeconds = 120;

    public boolean isConfigured() {
        return consumerKey != null && !consumerKey.isBlank()
                && consumerSecret != null && !consumerSecret.isBlank()
                && shortcode != null && !shortcode.isBlank()
                && passkey != null && !passkey.isBlank();
    }
}
