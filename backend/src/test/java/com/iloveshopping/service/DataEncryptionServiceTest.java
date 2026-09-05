package com.iloveshopping.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the AES-256-GCM at-rest encryption primitives used for order
 * addresses and payment callback data: round-trip, idempotency and
 * backward compatibility with legacy plaintext rows.
 */
class DataEncryptionServiceTest {

    private final DataEncryptionService encryption =
            new DataEncryptionService("test-key-material-for-unit-tests-only");

    @Test
    void roundTripPreservesPlaintext() {
        String addressJson = "{\"name\":\"John Doe\",\"city\":\"Nairobi\",\"phone\":\"+254700000000\"}";

        String stored = encryption.encrypt(addressJson);

        assertTrue(stored.startsWith("enc:v1:"), "stored value must carry the versioned prefix");
        assertEquals(addressJson, encryption.decrypt(stored));
    }

    @Test
    void encryptIsIdempotent() {
        String once = encryption.encrypt("sensitive");
        assertEquals(once, encryption.encrypt(once));
    }

    @Test
    void decryptPassesThroughLegacyPlaintext() {
        String legacy = "{\"name\":\"Jane\"}";
        assertEquals(legacy, encryption.decrypt(legacy));
    }

    @Test
    void decryptHandlesNull() {
        assertNull(encryption.decrypt(null));
    }

    @Test
    void samePlaintextEncryptsDifferently() {
        // Random IV per encryption — identical inputs must not match at rest.
        assertTrue(!encryption.encrypt("same").equals(encryption.encrypt("same")));
    }
}
