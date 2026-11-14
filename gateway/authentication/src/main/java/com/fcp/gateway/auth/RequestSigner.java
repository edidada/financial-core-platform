package com.fcp.gateway.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/** HMAC-SHA256 请求签名，配合时间戳与 nonce 防重放。 */
public final class RequestSigner {
    private RequestSigner() {}

    public static String canonical(String method, String path, String timestamp, String nonce, String body) {
        return method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + (body == null ? "" : body);
    }

    public static String sign(String secret, String canonical) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC unavailable", e);
        }
    }

    /** 常量时间比较，防时序侧信道 */
    public static boolean verify(String secret, String canonical, String provided) {
        String expected = sign(secret, canonical);
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = provided == null ? new byte[0] : provided.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}