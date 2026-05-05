package com.demo.packing.handler;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RouterHandler JWT validation via reflection
 * (validateJwt and helpers are private static — tested through reflection
 *  to avoid modifying production code for test purposes)
 */
class RouterHandlerTest {

    // ── helpers ──────────────────────────────────

    private String buildToken(String headerJson, String payloadJson, String sig) {
        String h = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String p = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return h + "." + p + "." + (sig != null ? sig : "fakesig");
    }

    private String validPayload(long expOffset) {
        long exp = Instant.now().getEpochSecond() + expOffset;
        return "{\"sub\":\"test\",\"companyId\":\"demo\",\"exp\":" + exp + "}";
    }

    private String hs256Header() {
        return "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    }

    private JsonObject invokeValidateJwt(String token) throws Exception {
        Method m = RouterHandler.class.getDeclaredMethod("validateJwt", String.class);
        m.setAccessible(true);
        return (JsonObject) m.invoke(null, token);
    }

    private String computeHmac(String input, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] sig = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
    }

    // ──────────────────────────────────────────────
    // JWT format
    // ──────────────────────────────────────────────

    @Test
    void validateJwt_validToken_shouldReturnPayloadClaims() throws Exception {
        String token = buildToken(hs256Header(), validPayload(3600), "fakesig");

        JsonObject claims = invokeValidateJwt(token);

        assertNotNull(claims);
        assertEquals("test", claims.getString("sub"));
        assertEquals("demo", claims.getString("companyId"));
    }

    @Test
    void validateJwt_missingParts_shouldThrow() {
        assertThrows(Exception.class, () -> invokeValidateJwt("only.twoparts"));
    }

    @Test
    void validateJwt_fourParts_shouldThrow() {
        assertThrows(Exception.class, () -> invokeValidateJwt("a.b.c.d"));
    }

    @Test
    void validateJwt_invalidBase64Header_shouldThrow() {
        assertThrows(Exception.class, () -> invokeValidateJwt("!!!.payload.sig"));
    }

    // ──────────────────────────────────────────────
    // Algorithm check
    // ──────────────────────────────────────────────

    @Test
    void validateJwt_algNone_shouldThrow() {
        String header = "{\"alg\":\"none\",\"typ\":\"JWT\"}";
        String token = buildToken(header, validPayload(3600), "fakesig");

        assertThrows(Exception.class, () -> invokeValidateJwt(token));
    }

    @Test
    void validateJwt_algBlank_shouldThrow() {
        String header = "{\"alg\":\"\",\"typ\":\"JWT\"}";
        String token = buildToken(header, validPayload(3600), "fakesig");

        assertThrows(Exception.class, () -> invokeValidateJwt(token));
    }

    // ──────────────────────────────────────────────
    // Expiry check
    // ──────────────────────────────────────────────

    @Test
    void validateJwt_expiredToken_shouldThrow() {
        String token = buildToken(hs256Header(), validPayload(-3600), "fakesig");

        assertThrows(Exception.class, () -> invokeValidateJwt(token));
    }

    @Test
    void validateJwt_missingExp_shouldThrow() {
        String payload = "{\"sub\":\"test\"}"; // no exp
        String token = buildToken(hs256Header(), payload, "fakesig");

        assertThrows(Exception.class, () -> invokeValidateJwt(token));
    }

    @Test
    void validateJwt_notYetActive_nbf_inFuture_shouldThrow() {
        long exp = Instant.now().getEpochSecond() + 3600;
        long nbf = Instant.now().getEpochSecond() + 7200; // nbf in future
        String payload = "{\"sub\":\"test\",\"exp\":" + exp + ",\"nbf\":" + nbf + "}";
        String token = buildToken(hs256Header(), payload, "fakesig");

        assertThrows(Exception.class, () -> invokeValidateJwt(token));
    }

    @Test
    void validateJwt_validNbf_inPast_shouldPass() throws Exception {
        long exp = Instant.now().getEpochSecond() + 3600;
        long nbf = Instant.now().getEpochSecond() - 60; // nbf already passed
        String payload = "{\"sub\":\"test\",\"exp\":" + exp + ",\"nbf\":" + nbf + "}";
        String token = buildToken(hs256Header(), payload, "fakesig");

        assertDoesNotThrow(() -> invokeValidateJwt(token));
    }

    // ──────────────────────────────────────────────
    // Signature check (with JWT_SECRET set)
    // ──────────────────────────────────────────────

    @Test
    void validateJwt_withSecret_validSignature_shouldPass() throws Exception {
        String secret = "test-secret";
        System.setProperty("JWT_SECRET", secret);

        try {
            String h = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(hs256Header().getBytes(StandardCharsets.UTF_8));
            String p = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(validPayload(3600).getBytes(StandardCharsets.UTF_8));
            String sig = computeHmac(h + "." + p, secret);
            String token = h + "." + p + "." + sig;

            JsonObject claims = invokeValidateJwt(token);
            assertNotNull(claims);
        } finally {
            System.clearProperty("JWT_SECRET");
        }
    }

    @Test
    void validateJwt_withSecret_wrongSignature_shouldThrow() {
        System.setProperty("JWT_SECRET", "test-secret");

        try {
            String token = buildToken(hs256Header(), validPayload(3600), "wrongsignature");
            assertThrows(Exception.class, () -> invokeValidateJwt(token));
        } finally {
            System.clearProperty("JWT_SECRET");
        }
    }

    @Test
    void validateJwt_withSecret_nonHS256Alg_shouldThrow() {
        System.setProperty("JWT_SECRET", "test-secret");

        try {
            String header = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
            String token = buildToken(header, validPayload(3600), "fakesig");
            assertThrows(Exception.class, () -> invokeValidateJwt(token));
        } finally {
            System.clearProperty("JWT_SECRET");
        }
    }

    // ──────────────────────────────────────────────
    // No secret mode — signature presence check
    // ──────────────────────────────────────────────

    @Test
    void validateJwt_noSecret_missingSignature_shouldThrow() {
        System.clearProperty("JWT_SECRET");
        String token = buildToken(hs256Header(), validPayload(3600), ""); // empty sig

        assertThrows(Exception.class, () -> invokeValidateJwt(token));
    }

    @Test
    void validateJwt_noSecret_anyNonEmptySignature_shouldPass() throws Exception {
        System.clearProperty("JWT_SECRET");
        String token = buildToken(hs256Header(), validPayload(3600), "anysig");

        assertDoesNotThrow(() -> invokeValidateJwt(token));
    }
}