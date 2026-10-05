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

    // One shared SecureRandom: SecureRandom is thread-safe, and constructing
    // a fresh generator per secret wastes entropy and flags DMI analysis.
    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();

    public static String generateSecret() {
        try {
            byte[] secretBytes = new byte[20];
            RANDOM.nextBytes(secretBytes);
            return encodeBase32(secretBytes);
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
            byte[] secretBytes = decodeSecret(secret);
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
            byte[] secretBytes = decodeSecret(secret);
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

    /**
     * Secrets issued before the base32 switch are 40-char hex; new secrets
     * are base32 (RFC 4648, no padding) — the format authenticator apps
     * (Google Authenticator, Authy) expect in the otpauth:// URI.
     */
    private static byte[] decodeSecret(String secret) {
        if (secret.matches("(?i)^[0-9a-f]{40}$")) {
            return hexToBytes(secret);
        }
        return decodeBase32(secret);
    }

    private static String encodeBase32(byte[] bytes) {
        char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
        StringBuilder sb = new StringBuilder((bytes.length * 8 + 4) / 5);
        for (int i = 0; i < bytes.length; i += 5) {
            long block = 0;
            int remaining = Math.min(5, bytes.length - i);
            for (int j = 0; j < remaining; j++) {
                block = (block << 8) | (bytes[i + j] & 0xFF);
            }
            block <<= (5 - remaining) * 8;
            int chars = (remaining * 8 + 4) / 5;
            for (int j = 0; j < chars; j++) {
                sb.append(alphabet[(int) ((block >> (35 - (j + 1) * 5)) & 0x1F)]);
            }
        }
        return sb.toString();
    }

    private static byte[] decodeBase32(String base32) {
        String cleaned = base32.replace("=", "").toUpperCase();
        java.util.BitSet bits = new java.util.BitSet();
        int bitCount = 0;
        for (char c : cleaned.toCharArray()) {
            int value = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".indexOf(c);
            if (value < 0) {
                throw new IllegalArgumentException("Invalid base32 character: " + c);
            }
            for (int b = 4; b >= 0; b--) {
                bits.set(bitCount++, (value >> b & 1) == 1);
            }
        }
        byte[] out = new byte[bitCount / 8];
        for (int i = 0; i < out.length; i++) {
            for (int b = 0; b < 8; b++) {
                if (bits.get(i * 8 + b)) {
                    out[i] |= (byte) (0x80 >> b);
                }
            }
        }
        return out;
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