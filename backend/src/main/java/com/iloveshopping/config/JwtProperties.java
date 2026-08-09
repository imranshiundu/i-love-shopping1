package com.iloveshopping.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    private String accessSecret;
    private String refreshSecret;
    private int accessExpiryMinutes = 15;
    private int refreshExpiryDays = 7;
    private String issuer = "i-love-shopping";
    private String audience = "i-love-shopping-api";

    public String getAccessSecret() {
        return accessSecret;
    }

    public void setAccessSecret(String accessSecret) {
        this.accessSecret = accessSecret;
    }

    public String getRefreshSecret() {
        return refreshSecret;
    }

    public void setRefreshSecret(String refreshSecret) {
        this.refreshSecret = refreshSecret;
    }

    public int getAccessExpiryMinutes() {
        return accessExpiryMinutes;
    }

    public void setAccessExpiryMinutes(int accessExpiryMinutes) {
        this.accessExpiryMinutes = accessExpiryMinutes;
    }

    public int getRefreshExpiryDays() {
        return refreshExpiryDays;
    }

    public void setRefreshExpiryDays(int refreshExpiryDays) {
        this.refreshExpiryDays = refreshExpiryDays;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public long getAccessExpiryMillis() {
        return (long) accessExpiryMinutes * 60 * 1000;
    }

    public long getRefreshExpiryMillis() {
        return (long) refreshExpiryDays * 24 * 60 * 60 * 1000;
    }
}