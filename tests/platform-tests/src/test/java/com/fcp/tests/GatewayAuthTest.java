package com.fcp.tests;

import com.fcp.gateway.auth.*;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class GatewayAuthTest {
    private static final String SECRET = "test-secret";

    @Test void signatureRoundTrip() {
        long now = System.currentTimeMillis();
        String canonical = RequestSigner.canonical("POST", "/v1/transactions",
                String.valueOf(now), "n1", "{}");
        String sig = RequestSigner.sign(SECRET, canonical);
        assertTrue(RequestSigner.verify(SECRET, canonical, sig));
        assertFalse(RequestSigner.verify(SECRET, canonical + "x", sig));
    }

    @Test void replayRejected() {
        var auth = new ApiKeyAuthenticator(Set.of("k1"), SECRET, Duration.ofMinutes(5));
        long now = System.currentTimeMillis();
        String ts = String.valueOf(now);
        String sig = RequestSigner.sign(SECRET, RequestSigner.canonical("POST", "/p", ts, "n1", "b"));
        assertTrue(auth.authenticate("k1", "POST", "/p", ts, "n1", "b", sig, now).authenticated());
        assertFalse(auth.authenticate("k1", "POST", "/p", ts, "n1", "b", sig, now).authenticated());
    }

    @Test void outsideWindowRejected() {
        var auth = new ApiKeyAuthenticator(Set.of("k1"), SECRET, Duration.ofMinutes(5));
        long now = System.currentTimeMillis();
        String old = String.valueOf(now - Duration.ofHours(1).toMillis());
        String sig = RequestSigner.sign(SECRET, RequestSigner.canonical("POST", "/p", old, "n2", "b"));
        assertFalse(auth.authenticate("k1", "POST", "/p", old, "n2", "b", sig, now).authenticated());
    }
}