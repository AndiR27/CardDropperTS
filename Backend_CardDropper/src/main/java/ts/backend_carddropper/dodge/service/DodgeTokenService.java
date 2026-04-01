package ts.backend_carddropper.dodge.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class DodgeTokenService {

    private static final String HMAC_ALGO = "HmacSHA256";

    private final byte[] secretBytes;

    public DodgeTokenService(@Value("${app.dodge.secret}") String secret) {
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Creates a signed token: "keycloakId:timestamp.signature"
     */
    public String createToken(String keycloakId) {
        long timestamp = System.currentTimeMillis();
        String payload = keycloakId + ":" + timestamp;
        String signature = sign(payload);
        return payload + "." + signature;
    }

    /**
     * Validates token and returns the embedded timestamp (ms).
     * Throws IllegalArgumentException if invalid.
     */
    public TokenData verify(String token, String expectedKeycloakId) {
        int dotIndex = token.lastIndexOf('.');
        if (dotIndex < 0) throw new IllegalArgumentException("Invalid token format");

        String payload = token.substring(0, dotIndex);
        String signature = token.substring(dotIndex + 1);

        // verify signature
        String expectedSig = sign(payload);
        if (!constantTimeEquals(signature, expectedSig)) {
            throw new IllegalArgumentException("Invalid token signature");
        }

        // parse payload "keycloakId:timestamp"
        int colonIndex = payload.lastIndexOf(':');
        if (colonIndex < 0) throw new IllegalArgumentException("Invalid token payload");

        String tokenKeycloakId = payload.substring(0, colonIndex);
        long timestamp = Long.parseLong(payload.substring(colonIndex + 1));

        // verify the token belongs to this user
        if (!tokenKeycloakId.equals(expectedKeycloakId)) {
            throw new IllegalArgumentException("Token does not match current user");
        }

        return new TokenData(tokenKeycloakId, timestamp);
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALGO));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC signing failed", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    public record TokenData(String keycloakId, long startedAtMs) {}
}
