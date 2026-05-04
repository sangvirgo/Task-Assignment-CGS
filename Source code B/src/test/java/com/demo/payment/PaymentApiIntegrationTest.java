package com.demo.payment;

import com.demo.packing.MainVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
class PaymentApiIntegrationTest {

    private static final int TEST_PORT = 18080;
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private String deploymentId;

    @BeforeEach
    void deployVerticle(Vertx vertx, VertxTestContext testContext) {
        System.setProperty("PORT", String.valueOf(TEST_PORT));
        vertx.deployVerticle(new MainVerticle())
                .onSuccess(id -> {
                    deploymentId = id;
                    testContext.completeNow();
                })
                .onFailure(testContext::failNow);
    }

    @AfterEach
    void undeployVerticle(Vertx vertx, VertxTestContext testContext) {
        if (deploymentId == null) {
            testContext.completeNow();
            return;
        }
        vertx.undeploy(deploymentId)
                .onSuccess(v -> testContext.completeNow())
                .onFailure(testContext::failNow);
    }

    @Test
    void paymentPreview_shouldReturn200_andCalculatedAmount() throws Exception {
        JsonObject body = new JsonObject()
                .put("orderId", "ORD-20001")
                .put("subTotal", 24.5)
                .put("shippingFee", 1.5)
                .put("voucherCode", "SAVE10")
                .put("paymentMethod", 0)
                .put("walletAmount", 3);

        HttpResponse<String> response = postJson("/api/v1/checkout/payment-preview", body);
        JsonObject json = new JsonObject(response.body());
        JsonObject data = json.getJsonObject("data");

        assertEquals(200, response.statusCode());
        assertEquals("Payment preview calculated", json.getString("message"));
        assertEquals("20.55", String.valueOf(data.getValue("payAmount")));
    }

    @Test
    void paymentPreview_shouldReturn400_whenVoucherInvalid() throws Exception {
        JsonObject body = new JsonObject()
                .put("orderId", "ORD-20002")
                .put("subTotal", 24.5)
                .put("shippingFee", 1.5)
                .put("voucherCode", "WRONG")
                .put("paymentMethod", 0)
                .put("walletAmount", 3);

        HttpResponse<String> response = postJson("/api/v1/checkout/payment-preview", body);
        JsonObject json = new JsonObject(response.body());

        assertEquals(400, response.statusCode());
        assertEquals("BAD_REQUEST", json.getString("code"));
        assertEquals("Invalid voucherCode", json.getString("message"));
    }

    @Test
    void confirm_shouldReturn200_andNullPaymentUrl_forCod() throws Exception {
        JsonObject body = new JsonObject()
                .put("orderId", "ORD-20003")
                .put("expectedPayAmount", 15)
                .put("paymentMethod", 1)
                .put("idempotencyKey", "idem-20003");

        HttpResponse<String> response = postJson("/api/v1/checkout/confirm", body);
        JsonObject json = new JsonObject(response.body());
        JsonObject data = json.getJsonObject("data");

        assertEquals(200, response.statusCode());
        assertEquals("Payment transaction created", json.getString("message"));
        assertEquals("PENDING", data.getString("paymentStatus"));
        assertNull(data.getValue("paymentUrl"));
    }

    @Test
    void pending_shouldReturn200_afterConfirmCreated() throws Exception {
        JsonObject confirmBody = new JsonObject()
                .put("orderId", "ORD-20004")
                .put("expectedPayAmount", 18)
                .put("paymentMethod", 0)
                .put("idempotencyKey", "idem-20004");

        HttpResponse<String> confirmResponse = postJson("/api/v1/checkout/confirm", confirmBody);
        JsonObject confirmJson = new JsonObject(confirmResponse.body());
        String transactionId = confirmJson.getJsonObject("data").getString("transactionId");

        HttpResponse<String> pendingResponse = get("/api/v1/checkout/pending/" + transactionId);
        JsonObject pendingJson = new JsonObject(pendingResponse.body());

        assertEquals(200, pendingResponse.statusCode());
        assertEquals("Payment pending status fetched", pendingJson.getString("message"));
        assertEquals(transactionId, pendingJson.getJsonObject("data").getString("transactionId"));
        assertEquals("PENDING", pendingJson.getJsonObject("data").getString("paymentStatus"));
    }

    @Test
    void pending_shouldReturn404_whenTransactionNotFound() throws Exception {
        HttpResponse<String> pendingResponse = get("/api/v1/checkout/pending/TXN_NOT_EXISTS");
        JsonObject pendingJson = new JsonObject(pendingResponse.body());

        assertEquals(404, pendingResponse.statusCode());
        assertEquals("NOT_FOUND", pendingJson.getString("code"));
        assertTrue(pendingJson.getString("message").contains("not found"));
    }

    @Test
    void paymentResult_shouldSetSuccess_whenPaymentSuccessful() throws Exception {
        JsonObject confirmBody = new JsonObject()
                .put("orderId", "ORD-20005")
                .put("expectedPayAmount", 19)
                .put("paymentMethod", 0)
                .put("idempotencyKey", "idem-20005");

        HttpResponse<String> confirmResponse = postJson("/api/v1/checkout/confirm", confirmBody);
        String transactionId = new JsonObject(confirmResponse.body()).getJsonObject("data").getString("transactionId");

        HttpResponse<String> resultResponse = postJson("/api/v1/checkout/payment-result", new JsonObject()
                .put("transactionId", transactionId)
                .put("success", true));

        JsonObject resultJson = new JsonObject(resultResponse.body());
        assertEquals(200, resultResponse.statusCode());
        assertEquals("SUCCESS", resultJson.getJsonObject("data").getString("paymentStatus"));
        assertEquals("SUCCESS", resultJson.getJsonObject("data").getString("orderStatus"));
    }

    @Test
    void paymentResult_shouldKeepOrderPending_whenPaymentFailedByWrongCard() throws Exception {
        JsonObject confirmBody = new JsonObject()
                .put("orderId", "ORD-20006")
                .put("expectedPayAmount", 21)
                .put("paymentMethod", 0)
                .put("idempotencyKey", "idem-20006");

        HttpResponse<String> confirmResponse = postJson("/api/v1/checkout/confirm", confirmBody);
        String transactionId = new JsonObject(confirmResponse.body()).getJsonObject("data").getString("transactionId");

        HttpResponse<String> resultResponse = postJson("/api/v1/checkout/payment-result", new JsonObject()
                .put("transactionId", transactionId)
                .put("success", false)
                .put("reason", "wrong card"));

        JsonObject resultJson = new JsonObject(resultResponse.body());
        assertEquals(200, resultResponse.statusCode());
        assertEquals("FAILED", resultJson.getJsonObject("data").getString("paymentStatus"));
        assertEquals("PENDING", resultJson.getJsonObject("data").getString("orderStatus"));
        assertEquals("wrong card", resultJson.getJsonObject("data").getString("failureReason"));
    }

    private HttpResponse<String> postJson(String path, JsonObject body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.encode()))
                .build();
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + TEST_PORT + path))
                .GET()
                .build();
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
