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

    public boolean isConfigured() {
        return consumerKey != null && !consumerKey.isBlank()
                && consumerSecret != null && !consumerSecret.isBlank()
                && shortcode != null && !shortcode.isBlank()
                && passkey != null && !passkey.isBlank();
    }
}
