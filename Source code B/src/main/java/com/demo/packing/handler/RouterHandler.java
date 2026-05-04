package com.demo.packing.handler;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

public class RouterHandler {

    private RouterHandler() {
    }

    public static void internalDecodeToken(RoutingContext context) {
        String authorization = context.request().getHeader("Authorization");

        if (authorization == null || authorization.isBlank()) {
            context.response()
                    .setStatusCode(401)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject()
                            .put("code", 401)
                            .put("message", "Missing Authorization header")
                            .encode());
            return;
        }

        String token = extractBearerToken(authorization);
        try {
            JsonObject claims = validateJwt(token);
            context.put("authToken", token);
            context.put("jwtClaims", claims);
            context.next();
        } catch (IllegalArgumentException ex) {
            context.response()
                    .setStatusCode(401)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject()
                            .put("code", 401)
                            .put("message", ex.getMessage())
                            .encode());
        }
    }

    private static String extractBearerToken(String authorization) {
        String trimmed = authorization.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = trimmed.substring(7).trim();
            if (token.isEmpty()) {
                throw new IllegalArgumentException("Bearer token is empty");
            }
            return token;
        }
        return trimmed;
    }

    private static JsonObject validateJwt(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }

        JsonObject header = decodeJwtPart(parts[0], "header");
        JsonObject payload = decodeJwtPart(parts[1], "payload");

        String algorithm = header.getString("alg", "");
        if (algorithm.isBlank() || "none".equalsIgnoreCase(algorithm)) {
            throw new IllegalArgumentException("Unsupported JWT algorithm");
        }

        validateTimeClaims(payload);
        validateSignatureIfConfigured(parts, algorithm);

        return payload;
    }

    private static JsonObject decodeJwtPart(String jwtPart, String sectionName) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(jwtPart);
            String json = new String(decoded, StandardCharsets.UTF_8);
            return new JsonObject(json);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid JWT " + sectionName);
        }
    }

    private static void validateTimeClaims(JsonObject payload) {
        Number exp = payload.getNumber("exp");
        if (exp == null) {
            throw new IllegalArgumentException("JWT exp claim is required");
        }

        long now = Instant.now().getEpochSecond();
        if (exp.longValue() <= now) {
            throw new IllegalArgumentException("JWT is expired");
        }

        Number nbf = payload.getNumber("nbf");
        if (nbf != null && nbf.longValue() > now) {
            throw new IllegalArgumentException("JWT is not active yet");
        }
    }

    private static void validateSignatureIfConfigured(String[] parts, String algorithm) {
        String secret = System.getProperty("JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            secret = System.getenv("JWT_SECRET");
        }

        if (secret == null || secret.isBlank()) {
            if (parts[2] == null || parts[2].isBlank()) {
                throw new IllegalArgumentException("JWT signature is missing");
            }
            return;
        }

        if (!"HS256".equalsIgnoreCase(algorithm)) {
            throw new IllegalArgumentException("Only HS256 is supported with JWT_SECRET");
        }

        String signingInput = parts[0] + "." + parts[1];
        byte[] expectedSignature = hmacSha256(signingInput, secret);
        byte[] tokenSignature;
        try {
            tokenSignature = Base64.getUrlDecoder().decode(parts[2]);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid JWT signature encoding");
        }

        if (!MessageDigest.isEqual(expectedSignature, tokenSignature)) {
            throw new IllegalArgumentException("JWT signature is invalid");
        }
    }

    private static byte[] hmacSha256(String signingInput, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cannot validate JWT signature");
        }
    }
}
