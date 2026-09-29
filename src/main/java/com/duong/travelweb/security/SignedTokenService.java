package com.duong.travelweb.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Token ký HMAC-SHA256, không lưu DB (VD link xác thực email). KHÔNG phải JWT nên resource server không thể nhận nhầm
 * làm access token; khoá HMAC tách theo mục đích (purpose) nên token của mục đích này không dùng được cho mục đích khác.
 * Dạng: base64url(userId \n expEpochSeconds \n email) "." base64url(hmac).
 */
@Component
public class SignedTokenService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final byte[] secret;

    public SignedTokenService(@Value("${app.jwt.secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public record Claims(UUID userId, String email) {
    }

    public String sign(String purpose, UUID userId, String email, long ttlSeconds) {
        long exp = Instant.now().getEpochSecond() + ttlSeconds;
        String payload = ENCODER.encodeToString((userId + "\n" + exp + "\n" + email).getBytes(StandardCharsets.UTF_8));
        return payload + "." + ENCODER.encodeToString(hmac(purpose, payload));
    }

    /** Rỗng nếu token sai định dạng, sai chữ ký, sai mục đích hoặc hết hạn. */
    public Optional<Claims> verify(String purpose, String token) {
        if (token == null) {
            return Optional.empty();
        }
        int dot = token.indexOf('.');
        if (dot <= 0 || dot == token.length() - 1) {
            return Optional.empty();
        }
        String payload = token.substring(0, dot);
        try {
            byte[] signature = DECODER.decode(token.substring(dot + 1));
            if (!MessageDigest.isEqual(signature, hmac(purpose, payload))) {
                return Optional.empty();
            }
            String[] parts = new String(DECODER.decode(payload), StandardCharsets.UTF_8).split("\n", 3);
            if (parts.length != 3 || Long.parseLong(parts[1]) < Instant.now().getEpochSecond()) {
                return Optional.empty();
            }
            return Optional.of(new Claims(UUID.fromString(parts[0]), parts[2]));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private byte[] hmac(String purpose, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] key = mac.doFinal(("tripnova:" + purpose).getBytes(StandardCharsets.UTF_8));
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
