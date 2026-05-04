package com.demo.payment;

import com.demo.payment.dto.PaymentConfirmReq;
import com.demo.payment.dto.PaymentPreviewReq;
import com.demo.payment.service.PaymentServiceImpl;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentServiceImplTest {

    private final PaymentServiceImpl paymentService = new PaymentServiceImpl();

    @Test
    void preview_shouldCalculateAmount_withVoucherAndWallet() {
        PaymentPreviewReq req = PaymentPreviewReq.fromJson(new JsonObject()
                .put("orderId", "ORD-10001")
                .put("subTotal", 24.5)
                .put("shippingFee", 1.5)
                .put("voucherCode", "SAVE10")
                .put("paymentMethod", 0)
                .put("walletAmount", 3));

        JsonObject result = paymentService.preview(req).toJson();

        assertEquals(new BigDecimal("2.45"), result.getValue("voucherDiscount"));
        assertEquals(new BigDecimal("3.00"), result.getValue("walletUsed"));
        assertEquals(new BigDecimal("20.55"), result.getValue("payAmount"));
        assertEquals("USD", result.getString("currency"));
    }

    @Test
    void preview_shouldCapWalletToGrossAmount() {
        PaymentPreviewReq req = PaymentPreviewReq.fromJson(new JsonObject()
                .put("orderId", "ORD-10002")
                .put("subTotal", 10)
                .put("shippingFee", 0)
                .put("paymentMethod", 1)
                .put("walletAmount", 20));

        JsonObject result = paymentService.preview(req).toJson();

        assertEquals(new BigDecimal("10.00"), result.getValue("walletUsed"));
        assertEquals(new BigDecimal("0.00"), result.getValue("payAmount"));
    }

    @Test
    void preview_shouldThrow_whenVoucherInvalid() {
        PaymentPreviewReq req = PaymentPreviewReq.fromJson(new JsonObject()
                .put("orderId", "ORD-10003")
                .put("subTotal", 20)
                .put("shippingFee", 2)
                .put("voucherCode", "BADCODE")
                .put("paymentMethod", 0)
                .put("walletAmount", 1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> paymentService.preview(req));
        assertEquals("Invalid voucherCode", ex.getMessage());
    }

    @Test
    void confirm_shouldReturnPaymentUrl_forAbaMethod() {
        PaymentConfirmReq req = PaymentConfirmReq.fromJson(new JsonObject()
                .put("orderId", "ORD-10004")
                .put("expectedPayAmount", 11.2)
                .put("paymentMethod", 0)
                .put("idempotencyKey", "idem-10004"));

        JsonObject result = paymentService.confirm(req).toJson();

        assertEquals("PENDING", result.getString("paymentStatus"));
        assertTrue(result.getString("transactionId").startsWith("TXN"));
        assertTrue(result.getString("paymentUrl").contains("/checkout/TXN"));
    }

    @Test
    void confirm_shouldReturnNullPaymentUrl_forCodMethod() {
        PaymentConfirmReq req = PaymentConfirmReq.fromJson(new JsonObject()
                .put("orderId", "ORD-10005")
                .put("expectedPayAmount", 15)
                .put("paymentMethod", 1)
                .put("idempotencyKey", "idem-10005"));

        JsonObject result = paymentService.confirm(req).toJson();

        assertEquals("PENDING", result.getString("paymentStatus"));
        assertNull(result.getValue("paymentUrl"));
    }

    @Test
    void pending_shouldReturnSavedPendingTransaction() {
        PaymentConfirmReq confirmReq = PaymentConfirmReq.fromJson(new JsonObject()
                .put("orderId", "ORD-10006")
                .put("expectedPayAmount", 12)
                .put("paymentMethod", 0)
                .put("idempotencyKey", "idem-10006"));

        String transactionId = paymentService.confirm(confirmReq).toJson().getString("transactionId");
        JsonObject pending = paymentService.pending(transactionId).toJson();

        assertEquals(transactionId, pending.getString("transactionId"));
        assertEquals("PENDING", pending.getString("paymentStatus"));
    }

    @Test
    void applyPaymentResult_shouldSetSuccess_whenPaymentSuccess() {
        PaymentConfirmReq confirmReq = PaymentConfirmReq.fromJson(new JsonObject()
                .put("orderId", "ORD-10007")
                .put("expectedPayAmount", 12)
                .put("paymentMethod", 0)
                .put("idempotencyKey", "idem-10007"));

        String transactionId = paymentService.confirm(confirmReq).toJson().getString("transactionId");
        JsonObject updated = paymentService.applyPaymentResult(com.demo.payment.dto.PaymentResultReq.fromJson(new JsonObject()
                .put("transactionId", transactionId)
                .put("success", true))).toJson();

        assertEquals("SUCCESS", updated.getString("paymentStatus"));
        assertEquals("SUCCESS", updated.getString("orderStatus"));
    }

    @Test
    void applyPaymentResult_shouldKeepOrderPending_whenPaymentFails() {
        PaymentConfirmReq confirmReq = PaymentConfirmReq.fromJson(new JsonObject()
                .put("orderId", "ORD-10008")
                .put("expectedPayAmount", 12)
                .put("paymentMethod", 0)
                .put("idempotencyKey", "idem-10008"));

        String transactionId = paymentService.confirm(confirmReq).toJson().getString("transactionId");
        JsonObject updated = paymentService.applyPaymentResult(com.demo.payment.dto.PaymentResultReq.fromJson(new JsonObject()
                .put("transactionId", transactionId)
                .put("success", false)
                .put("reason", "wrong card"))).toJson();

        assertEquals("FAILED", updated.getString("paymentStatus"));
        assertEquals("PENDING", updated.getString("orderStatus"));
        assertEquals("wrong card", updated.getString("failureReason"));
    }
}
