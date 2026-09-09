package com.iloveshopping.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Mode-safety: test keys must never leak into live mode (or vice versa),
 * otherwise checkout would badge the wrong money semantics.
 */
class StripePropertiesTest {

    private StripeProperties props(String env, String secret, String pub,
                                   String testSecret, String testPub,
                                   String liveSecret, String livePub) {
        StripeProperties p = new StripeProperties();
        p.setEnvironment(env);
        p.setSecretKey(secret);
        p.setPublishableKey(pub);
        p.setTestSecretKey(testSecret);
        p.setTestPublishableKey(testPub);
        p.setLiveSecretKey(liveSecret);
        p.setLivePublishableKey(livePub);
        return p;
    }

    @Test
    void testModePrefersTestKeys() {
        StripeProperties p = props("test", "sk_test_legacy", "pk_test_legacy",
                "sk_test_mode", "pk_test_mode", "sk_live_mode", "pk_live_mode");
        assertEquals("sk_test_mode", p.effectiveSecretKey());
        assertEquals("pk_test_mode", p.effectivePublishableKey());
    }

    @Test
    void liveModePrefersLiveKeys() {
        StripeProperties p = props("live", "sk_test_legacy", "pk_test_legacy",
                "sk_test_mode", "pk_test_mode", "sk_live_mode", "pk_live_mode");
        assertEquals("sk_live_mode", p.effectiveSecretKey());
        assertEquals("pk_live_mode", p.effectivePublishableKey());
    }

    @Test
    void liveModeRejectsTestKeys() {
        StripeProperties p = props("live", "sk_test_legacy", "pk_test_legacy",
                null, null, null, null);
        assertNull(p.effectiveSecretKey());
        assertNull(p.effectivePublishableKey());
    }

    @Test
    void testModeRejectsLiveKeys() {
        StripeProperties p = props("test", "sk_live_legacy", "pk_live_legacy",
                null, null, null, null);
        assertNull(p.effectiveSecretKey());
        assertNull(p.effectivePublishableKey());
    }

    @Test
    void legacyKeysHonoredWhenPrefixMatchesMode() {
        StripeProperties test = props("test", "sk_test_abc", "pk_test_abc", null, null, null, null);
        assertEquals("sk_test_abc", test.effectiveSecretKey());
        assertEquals("pk_test_abc", test.effectivePublishableKey());

        StripeProperties live = props("live", "sk_live_abc", "pk_live_abc", null, null, null, null);
        assertEquals("sk_live_abc", live.effectiveSecretKey());
        assertEquals("pk_live_abc", live.effectivePublishableKey());
    }

    @Test
    void unconfiguredWhenBlank() {
        StripeProperties p = props("test", null, null, null, null, null, null);
        assertNull(p.effectiveSecretKey());
        assertNull(p.effectivePublishableKey());
    }
}
