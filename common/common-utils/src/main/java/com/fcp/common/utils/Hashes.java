package com.fcp.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** 幂等键请求参数指纹。 */
public final class Hashes {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Hashes() {}

    public static String sha256(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            char[] out = new char[digest.length * 2];
            for (int i = 0; i < digest.length; i++) {
                out[i * 2] = HEX[(digest[i] >>> 4) & 0xF];
                out[i * 2 + 1] = HEX[digest[i] & 0xF];
            }
            return new String(out);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}