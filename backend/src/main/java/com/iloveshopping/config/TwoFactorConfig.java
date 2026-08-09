package com.iloveshopping.config;

public class TwoFactorConfig {

    public static final int DEFAULT_TIME_STEP = 30;
    public static final int DEFAULT_CODE_DIGITS = 6;
    public static final int DEFAULT_CODE_TIMEOUT_SECONDS = 30;
    public static final String ISSUER = "i-love-shopping";

    private TwoFactorConfig() {
    }
}