package com.iloveshopping.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "security.recaptcha")
public class RecaptchaProperties {

    private String secretKey;
    private String siteKey;
    private String verifyUrl = "https://www.google.com/recaptcha/api/siteverify";
}