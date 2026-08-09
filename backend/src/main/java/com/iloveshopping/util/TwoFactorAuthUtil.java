package com.iloveshopping.util;

import com.iloveshopping.config.TwoFactorConfig;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Slf4j
public class TwoFactorAuthUtil {

    private static final String QR_PREFIX = "otpauth://totp/";
    private static final String HMAC_SHA1 = "HmacSHA1";

    public static String generateSecret() {
        try {
            byte[] secretBytes = new byte[20];
            java.security.SecureRandom random = new java.security.SecureRandom();
            random.nextBytes(secretBytes);
            return bytesToHex(secretBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate 2FA secret", e);
        }
    }

    public static String getQrCodeUrl(String email, String secret) {
        return QR_PREFIX + TwoFactorConfig.ISSUER + ":" + email
                + "?secret=" + secret
                + "&issuer=" + TwoFactorConfig.ISSUER
                + "&algorithm=SHA1"
                + "&digits=" + TwoFactorConfig.DEFAULT_CODE_DIGITS
                + "&period=" + TwoFactorConfig.DEFAULT_TIME_STEP;
    }

    public static boolean verifyCode(String secret, String code) {
        if (secret == null || code == null) {
            return false;
        }
        try {
            byte[] secretBytes = hexToBytes(secret);
            return verifyCode(secretBytes, code, System.currentTimeMillis() / 1000L);
        } catch (Exception e) {
            log.warn("2FA code verification failed: {}", e.getMessage());
            return false;
        }
    }

    public static boolean verifyTimeBasedCode(String secret, String code, long time) {
        if (secret == null || code == null) {
            return false;
        }
        try {
            byte[] secretBytes = hexToBytes(secret);
            return verifyCode(secretBytes, code, time);
        } catch (Exception e) {
            log.warn("2FA time-based code verification failed: {}", e.getMessage());
            return false;
        }
    }

    private static boolean verifyCode(byte[] secret, String code, long time) {
        try {
            int timeStep = TwoFactorConfig.DEFAULT_TIME_STEP;
            long counter = time / timeStep;

            // Allow one step before and after for clock drift
            for (int i = -1; i <= 1; i++) {
                long testCounter = counter + i;
                String generatedCode = generateTotp(secret, testCounter, TwoFactorConfig.DEFAULT_CODE_DIGITS);
                if (generatedCode.equals(code)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.warn("TOTP verification failed: {}", e.getMessage());
            return false;
        }
    }

    private static String generateTotp(byte[] secret, long movingFactor, int digits) {
        try {
            byte[] counterBytes = new byte[8];
            long reverse = Long.reverseBytes(movingFactor);
            for (int i = 0; i < 8; i++) {
                counterBytes[i] = (byte) (reverse >> (i * 8));
            }

            Mac mac = Mac.getInstance(HMAC_SHA1);
            SecretKeySpec keySpec = new SecretKeySpec(secret, HMAC_SHA1);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(counterBytes);

            int offset = hash[hash.length - 1] & 0xf;
            int binary = ((hash[offset] & 0x7f) << 24) |
                         ((hash[offset + 1] & 0xff) << 16) |
                         ((hash[offset + 2] & 0xff) << 8) |
                         (hash[offset + 3] & 0xff);

            int otp = binary % (int) Math.pow(10, digits);
            return String.format("%0" + digits + "d", otp);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate TOTP", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}