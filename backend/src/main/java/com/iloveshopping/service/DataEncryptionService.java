package com.iloveshopping.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class DataEncryptionService {

    private static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey secretKey;
    private final SecureRandom random = new SecureRandom();
    private static volatile DataEncryptionService instance;

    public DataEncryptionService(@Value("${app.data-encryption-key:}") String keyMaterial) {
        if (keyMaterial == null || keyMaterial.isBlank()) {
            throw new IllegalStateException(
                    "DATA_ENCRYPTION_KEY is not configured. Sensitive data is encrypted at rest; "
                            + "the key must be supplied via .env (see .env.example) — a hardcoded "
                            + "fallback key would let anyone with the source decrypt every database.");
        }
        byte[] keyBytes = normalize(keyMaterial);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Deterministic keyed hash used for equality lookups over encrypted
     * columns (login by email, guest-order claiming). HMAC-SHA256 with the
     * data-encryption key: same input -> same hash, never reversible to the
     * plaintext, and it never reuses the AES encryption key.
     */
    public String lookupHash(String plain) {
        if (plain == null || plain.isBlank()) {
            return null;
        }
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(plain.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute lookup hash", e);
        }
    }

    @PostConstruct
    void publishInstance() {
        DataEncryptionService.instance = this;
    }

    public static String decryptStatic(String stored) {
        DataEncryptionService svc = instance;
        if (svc == null) return stored;
        return svc.decrypt(stored);
    }

    public static String encryptStatic(String plain) {
        DataEncryptionService svc = instance;
        if (svc == null) return plain;
        return svc.encrypt(plain);
    }

    /** Deterministic lookup hash usable wherever the bean can't be injected. */
    public static String lookupHashStatic(String plain) {
        DataEncryptionService svc = instance;
        if (svc == null) {
            throw new IllegalStateException("DataEncryptionService not initialized");
        }
        return svc.lookupHash(plain);
    }

    public static String encryptForJson(String plain) {
        String cipher = encryptStatic(plain);
        return "\"" + cipher + "\"";
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isBlank() || plain.startsWith(PREFIX)) {
            return plain;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt sensitive field", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null) return null;
        String t = stored.trim();
        if (t.length() > 1 && t.startsWith("\"") && t.endsWith("\"")) {
            t = t.substring(1, t.length() - 1);
        }
        if (!t.startsWith(PREFIX)) {
            return stored;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(t.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, combined, 0, IV_LENGTH));
            byte[] plain = cipher.doFinal(combined, IV_LENGTH, combined.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt sensitive field", e);
        }
    }

    private byte[] normalize(String material) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return digest.digest(material.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to derive encryption key", e);
        }
    }
}
