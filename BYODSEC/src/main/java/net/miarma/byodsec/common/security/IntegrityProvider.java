package net.miarma.byodsec.common.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class IntegrityProvider {
    public static String generateHMAC(String key, String data) throws Exception {
        Mac sha256HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256HMAC.init(secretKey);
        return byteArrayToHex(sha256HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    public static String generateNonce() {
        byte[] nonce = new byte[16];
        new SecureRandom().nextBytes(nonce);
        return Base64.getEncoder().encodeToString(nonce);
    }

    public static String byteArrayToHex(byte[] arr) {
        StringBuilder sb = new StringBuilder(arr.length * 2);
        for(byte b : arr) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
