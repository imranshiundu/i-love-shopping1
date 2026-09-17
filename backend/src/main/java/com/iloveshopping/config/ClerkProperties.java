package com.iloveshopping.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "clerk")
public class ClerkProperties {

    /**
     * JWKS endpoint of the Clerk instance, e.g.
     * https://your-app.clerk.accounts.dev/.well-known/jwks.json
     */
    private String jwksUrl = "";

    /**
     * Clerk secret key (sk_test_... / sk_live_...) used to fetch the
     * user's profile from the Clerk Backend API after token verification.
     */
    private String secretKey = "";

    private boolean enabled = true;

    public String getJwksUrl() {
        return jwksUrl;
    }

    public void setJwksUrl(String jwksUrl) {
        this.jwksUrl = jwksUrl;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isConfigured() {
        return enabled && jwksUrl != null && !jwksUrl.isBlank()
                && secretKey != null && !secretKey.isBlank();
    }
}
