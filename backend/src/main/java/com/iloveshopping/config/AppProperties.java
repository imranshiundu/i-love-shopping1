package com.iloveshopping.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String frontendUrl = "http://localhost:3000";
    private String apiUrl = "http://localhost:8080/api/v1";
    private String orderNumberPrefix = "ILS";
    private String defaultCurrency = "KES";
    private double taxRate = 0.16;
    private double freeShippingThreshold = 5000;
    private int maxCartItems = 50;
    private int maxCartQuantityPerItem = 99;
    private String mailFrom = "noreply@iloveshopping.com";

    public boolean isDevMode() {
        return "development".equals(System.getProperty("spring.profiles.active"));
    }
}