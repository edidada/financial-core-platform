package com.fcp.gateway.auth;

import java.time.Duration;
import java.util.Set;

/** API Key + 签名 + 防重放三合一认证器。 */
public final class ApiKeyAuthenticator {
    private final Set<String> allowedKeys;
    private final String sharedSecret;
    private final ReplayGuard replayGuard;

    public ApiKeyAuthenticator(Set<String> allowedKeys, String sharedSecret, Duration replayWindow) {
        this.allowedKeys = Set.copyOf(allowedKeys);
        this.sharedSecret = sharedSecret;
        this.replayGuard = new ReplayGuard(replayWindow);
    }

    public AuthResult authenticate(String apiKey, String method, String path,
                                   String timestamp, String nonce, String body, String signature,
                                   long nowMillis) {
        if (apiKey == null || !allowedKeys.contains(apiKey)) return AuthResult.reject("unknown api key");
        if (timestamp == null) return AuthResult.reject("missing timestamp");
        long ts;
        try { ts = Long.parseLong(timestamp); }
        catch (NumberFormatException e) { return AuthResult.reject("bad timestamp"); }
        if (!replayGuard.accept(nonce, ts, nowMillis)) return AuthResult.reject("replay detected");
        String canonical = RequestSigner.canonical(method, path, timestamp, nonce, body);
        if (!RequestSigner.verify(sharedSecret, canonical, signature)) return AuthResult.reject("bad signature");
        return AuthResult.ok(apiKey);
    }
}