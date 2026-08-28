package com.iloveshopping.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "flutterwave")
public class FlutterwaveProperties {
    private String secretKey;
    private String publicKey;
    private String encryptionKey;
    private String redirectUrl = "http://localhost:3000/checkout/success";
}
