package com.demo.payment;

import com.demo.payment.dto.PaymentConfirmReq;
import com.demo.payment.dto.PaymentConfirmRes;
import com.demo.payment.dto.PaymentPendingRes;
import com.demo.payment.dto.PaymentPreviewReq;
import com.demo.payment.dto.PaymentPreviewRes;
import com.demo.payment.dto.PaymentResultReq;
import com.demo.payment.service.PaymentServiceImpl;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PaymentServiceImplTest {

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl();
    }

    // ──────────────────────────────────────────────
    // preview — happy path
    // ──────────────────────────────────────────────

    @Test
    void preview_noVoucherNoWallet_payAmount_shouldEqual_subTotal_plus_shippingFee() {
        PaymentPreviewReq req = PaymentPreviewReq.fromJson(new JsonObject()
                .put("orderId", "ORD-001")
                .put("subTotal", 50.0)
                .put("shippingFee", 5.0)
                .put("paymentMethod", 0));

        PaymentPreviewRes res = paymentService.preview(req);

        assertEquals(0, BigDecimal.valueOf(55.00).compareTo(res.toJson().getJsonNumber("payAmount").bigDecimalValue()));
    }

    @Test
    void preview_withSave10Voucher_shouldDeduct10Percent() {
        PaymentPreviewReq req = PaymentPreviewReq.fromJson(new JsonObject()
                .put("orderId", "ORD-002")
                .put("subTotal", 100.0)
                .put("shippingFee", 0.0)
                .put("voucherCode", "SAVE10")
                .put("paymentMethod", 0));

        JsonObject res = paymentService.preview(req).toJson();

        assertEquals(0, BigDecimal.valueOf(10.00).compareTo(res.getJsonNumber("voucherDiscount").bigDecimalValue()));
        assertEquals(0, BigDecimal.valueOf(90.00).compareTo(res.getJsonNumber("payAmount").bigDecimalValue()));
    }

    @Test
    void preview_withSave5Voucher_shouldDeduct5Percent() {
        PaymentPreviewReq req = PaymentPreviewReq.fromJson(new JsonObject()
                .put("orderId", "ORD-003")
                .put("subTotal", 200.0)
                .put("shippingFee", 0.0)
                .put("voucherCode", "SAVE5")
                .put("paymentMethod", 1));

        JsonObject res = paymentService.preview(req).toJson();

        assertEquals(0, BigDecimal.valueOf(10.00).compareTo(res.getJsonNumber("voucherDiscount").bigDecimalValue()));
        assertEquals(0, BigDecimal.valueOf(190.00).compareTo(res.getJsonNumber("payAmount").bigDecimalValue()));
    }

    @Test
    void preview_walletCoversAll_payAmount_shouldBeZero() {
        PaymentPreviewReq req = PaymentPreviewReq.fromJson(new JsonObject()
                .put("orderId", "ORD-004")
                .put("subTotal", 10.0)
                .put("shippingFee", 0.0)
                .put("paymentMethod", 1)
                .put("walletAmount", 50.0));

        JsonObject res = paymentService.preview(req).toJson();

        assertEquals(0, BigDecimal.valueOf(10.00).compareTo(res.getJsonNumber("walletUsed").bigDecimalValue()));
        assertEquals(0, BigDecimal.ZERO.compareTo(res.getJsonNumber("payAmount").bigDecimalValue()));
    }

    @Test
    void preview_walletPartial_payAmount_shouldBeGrossMinusWallet() {
        PaymentPreviewReq req = PaymentPreviewReq.fromJson(new JsonObject()
                .put("orderId", "ORD-005")
                .put("subTotal", 24.5)
                .put("shippingFee", 1.5)
                .put("voucherCode", "SAVE10")
                .put("paymentMethod", 0)
                .put("walletAmount", 3.0));

        JsonObject res = paymentService.preview(req).toJson();

        assertEquals(0, BigDecimal.valueOf(20.55).compareTo(res.getJsonNumber("payAmount").bigDecimalValue()));
    }

    @Test
    void preview_currency_shouldBeUSD() {
        PaymentPreviewReq req = PaymentPreviewReq.fromJson(new JsonObject()
                .put("orderId", "ORD-006")
                .put("subTotal", 10.0)
                .put("shippingFee", 0.0)
                .put("paymentMethod", 0));

        assertEquals("USD", paymentService.preview(req).toJson().getString("currency"));
    }

    @Test
    void preview_voucherCodeCaseInsensitive_shouldWork() {
        PaymentPreviewReq req = PaymentPreviewReq.fromJson(new JsonObject()
                .put("orderId", "ORD-007")
                .put("subTotal", 100.0)
                .put("shippingFee", 0.0)
                .put("voucherCode", "save10")
                .put("paymentMethod", 0));

        JsonObject res = paymentService.preview(req).toJson();
        assertEquals(0, BigDecimal.valueOf(10.00).compareTo(res.getJsonNumber("voucherDiscount").bigDecimalValue()));
    }

    // ──────────────────────────────────────────────
    // preview — validation errors
    // ──────────────────────────────────────────────

    @Test
    void preview_missingOrderId_shouldThrow() {
        PaymentPreviewReq req = PaymentPreviewReq.fromJson(new JsonObject()
                .put("subTotal", 10.0)
                .put("shippingFee", 0.0)
                .put("paymentMethod", 0));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> paymentService.preview(req));
        assertEquals("orderId is required", ex.getMessage());
    }

    @Test
    void preview_invalidVoucherCode_shouldThrow() {
        PaymentPreviewReq req = PaymentPreviewReq.fromJson(new JsonObject()
                .put("orderId", "ORD-008")
                .put("subTotal", 10.0)
                .put("shippingFee", 0.0)
                .put("voucherCode", "BADCODE")
                .put("paymentMethod", 0));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> paymentService.preview(req));
        assertEquals("Invalid voucherCode", ex.getMessage());
    }

    @Test
    void preview_negativeSubTotal_shouldThrow() {
        PaymentPreviewReq req = PaymentPreviewReq.fromJson(new JsonObject()
                .put("orderId", "ORD-009")
                .put("subTotal", -5.0)
                .put("shippingFee", 0.0)
                .put("paymentMethod", 0));

        assertThrows(IllegalArgumentException.class, () -> paymentService.preview(req));
    }

    @Test
    void preview_negativeShippingFee_shouldThrow() {
        PaymentPreviewReq req = PaymentPreviewReq.fromJson(new JsonObject()
                .put("orderId", "ORD-010")
                .put("subTotal", 10.0)
                .put("shippingFee", -1.0)
                .put("paymentMethod", 0));

        assertThrows(IllegalArgumentException.class, () -> paymentService.preview(req));
    }

    @Test
    void preview_invalidPaymentMethod_shouldThrow() {
        PaymentPreviewReq req = PaymentPreviewReq.fromJson(new JsonObject()
                .put("orderId", "ORD-011")
                .put("subTotal", 10.0)
                .put("shippingFee", 0.0)
                .put("paymentMethod", 99));

        assertThrows(IllegalArgumentException.class, () -> paymentService.preview(req));
    }

    // ──────────────────────────────────────────────
    // confirm — happy path
    // ──────────────────────────────────────────────

    @Test
    void confirm_abaMethod_shouldReturnPaymentUrl() {
        PaymentConfirmReq req = PaymentConfirmReq.fromJson(new JsonObject()
                .put("orderId", "ORD-012")
                .put("expectedPayAmount", 50.0)
                .put("paymentMethod", 0)
                .put("idempotencyKey", "idem-012"));

        JsonObject res = paymentService.confirm(req).toJson();

        assertEquals("PENDING", res.getString("paymentStatus"));
        assertTrue(res.getString("transactionId").startsWith("TXN"));
        assertNotNull(res.getString("paymentUrl"));
        assertTrue(res.getString("paymentUrl").contains("/checkout/TXN"));
    }

    @Test
    void confirm_codMethod_paymentUrl_shouldBeNull() {
        PaymentConfirmReq req = PaymentConfirmReq.fromJson(new JsonObject()
                .put("orderId", "ORD-013")
                .put("expectedPayAmount", 50.0)
                .put("paymentMethod", 1)
                .put("idempotencyKey", "idem-013"));

        JsonObject res = paymentService.confirm(req).toJson();

        assertEquals("PENDING", res.getString("paymentStatus"));
        assertNull(res.getValue("paymentUrl"));
    }

    @Test
    void confirm_shouldSetExpiredAt_inFuture() {
        PaymentConfirmReq req = PaymentConfirmReq.fromJson(new JsonObject()
                .put("orderId", "ORD-014")
                .put("expectedPayAmount", 10.0)
                .put("paymentMethod", 0)
                .put("idempotencyKey", "idem-014"));

        JsonObject res = paymentService.confirm(req).toJson();

        long now = System.currentTimeMillis();
        assertTrue(res.getLong("expiredAt") > now);
    }

    // ──────────────────────────────────────────────
    // confirm — validation errors
    // ──────────────────────────────────────────────

    @Test
    void confirm_missingOrderId_shouldThrow() {
        PaymentConfirmReq req = PaymentConfirmReq.fromJson(new JsonObject()
                .put("expectedPayAmount", 10.0)
                .put("paymentMethod", 0)
                .put("idempotencyKey", "idem-015"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> paymentService.confirm(req));
        assertEquals("orderId is required", ex.getMessage());
    }

    @Test
    void confirm_missingIdempotencyKey_shouldThrow() {
        PaymentConfirmReq req = PaymentConfirmReq.fromJson(new JsonObject()
                .put("orderId", "ORD-016")
                .put("expectedPayAmount", 10.0)
                .put("paymentMethod", 0));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> paymentService.confirm(req));
        assertEquals("idempotencyKey is required", ex.getMessage());
    }

    @Test
    void confirm_negativeExpectedPayAmount_shouldThrow() {
        PaymentConfirmReq req = PaymentConfirmReq.fromJson(new JsonObject()
                .put("orderId", "ORD-017")
                .put("expectedPayAmount", -1.0)
                .put("paymentMethod", 0)
                .put("idempotencyKey", "idem-017"));

        assertThrows(IllegalArgumentException.class, () -> paymentService.confirm(req));
    }

    // ──────────────────────────────────────────────
    // pending
    // ──────────────────────────────────────────────

    @Test
    void pending_afterConfirm_shouldReturnPendingStatus() {
        PaymentConfirmReq req = PaymentConfirmReq.fromJson(new JsonObject()
                .put("orderId", "ORD-018")
                .put("expectedPayAmount", 10.0)
                .put("paymentMethod", 0)
                .put("idempotencyKey", "idem-018"));

        String txnId = paymentService.confirm(req).toJson().getString("transactionId");
        JsonObject res = paymentService.pending(txnId).toJson();

        assertEquals(txnId, res.getString("transactionId"));
        assertEquals("PENDING", res.getString("paymentStatus"));
        assertEquals("PENDING", res.getString("orderStatus"));
    }

    @Test
    void pending_nonExistentTransaction_shouldThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> paymentService.pending("TXN_NOT_EXIST"));
        assertEquals("Transaction not found", ex.getMessage());
    }

    @Test
    void pending_nullTransactionId_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> paymentService.pending(null));
    }

    @Test
    void pending_blankTransactionId_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> paymentService.pending("  "));
    }

    // ──────────────────────────────────────────────
    // applyPaymentResult — success
    // ──────────────────────────────────────────────

    @Test
    void applyPaymentResult_success_shouldSetBothStatusToSuccess() {
        String txnId = confirmAndGetTxnId("ORD-019", "idem-019");

        JsonObject res = paymentService.applyPaymentResult(
                PaymentResultReq.fromJson(new JsonObject()
                        .put("transactionId", txnId)
                        .put("success", true))
        ).toJson();

        assertEquals("SUCCESS", res.getString("paymentStatus"));
        assertEquals("SUCCESS", res.getString("orderStatus"));
    }

    @Test
    void applyPaymentResult_success_expiredAt_shouldBePreserved() {
        String txnId = confirmAndGetTxnId("ORD-020", "idem-020");
        Long expiredAt = paymentService.pending(txnId).toJson().getLong("expiredAt");

        JsonObject res = paymentService.applyPaymentResult(
                PaymentResultReq.fromJson(new JsonObject()
                        .put("transactionId", txnId)
                        .put("success", true))
        ).toJson();

        assertEquals(expiredAt, res.getLong("expiredAt"));
    }

    // ──────────────────────────────────────────────
    // applyPaymentResult — failure
    // ──────────────────────────────────────────────

    @Test
    void applyPaymentResult_failure_paymentStatus_shouldBeFailed_orderStatus_shouldBePending() {
        String txnId = confirmAndGetTxnId("ORD-021", "idem-021");

        JsonObject res = paymentService.applyPaymentResult(
                PaymentResultReq.fromJson(new JsonObject()
                        .put("transactionId", txnId)
                        .put("success", false)
                        .put("reason", "insufficient funds"))
        ).toJson();

        assertEquals("FAILED", res.getString("paymentStatus"));
        assertEquals("PENDING", res.getString("orderStatus"));
        assertEquals("insufficient funds", res.getString("failureReason"));
    }

    @Test
    void applyPaymentResult_failure_missingReason_shouldThrow() {
        String txnId = confirmAndGetTxnId("ORD-022", "idem-022");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> paymentService.applyPaymentResult(
                        PaymentResultReq.fromJson(new JsonObject()
                                .put("transactionId", txnId)
                                .put("success", false))
                ));
        assertEquals("reason is required when payment fails", ex.getMessage());
    }

    @Test
    void applyPaymentResult_nonExistentTransaction_shouldThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> paymentService.applyPaymentResult(
                        PaymentResultReq.fromJson(new JsonObject()
                                .put("transactionId", "TXN_NOT_EXIST")
                                .put("success", true))
                ));
        assertEquals("Transaction not found", ex.getMessage());
    }

    @Test
    void applyPaymentResult_missingTransactionId_shouldThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> paymentService.applyPaymentResult(
                        PaymentResultReq.fromJson(new JsonObject()
                                .put("success", true))
                ));
        assertEquals("transactionId is required", ex.getMessage());
    }

    // ──────────────────────────────────────────────
    // full flow
    // ──────────────────────────────────────────────

    @Test
    void fullFlow_preview_confirm_pending_result_shouldWorkEndToEnd() {
        // Preview
        PaymentPreviewReq previewReq = PaymentPreviewReq.fromJson(new JsonObject()
                .put("orderId", "ORD-FLOW")
                .put("subTotal", 80.0)
                .put("shippingFee", 5.0)
                .put("voucherCode", "SAVE10")
                .put("paymentMethod", 0)
                .put("walletAmount", 10.0));
        JsonObject preview = paymentService.preview(previewReq).toJson();
        assertNotNull(preview.getValue("payAmount"));

        // Confirm
        PaymentConfirmReq confirmReq = PaymentConfirmReq.fromJson(new JsonObject()
                .put("orderId", "ORD-FLOW")
                .put("expectedPayAmount", preview.getValue("payAmount"))
                .put("paymentMethod", 0)
                .put("idempotencyKey", "idem-flow"));
        String txnId = paymentService.confirm(confirmReq).toJson().getString("transactionId");
        assertNotNull(txnId);

        // Pending
        assertEquals("PENDING", paymentService.pending(txnId).toJson().getString("paymentStatus"));

        // Result
        JsonObject result = paymentService.applyPaymentResult(
                PaymentResultReq.fromJson(new JsonObject()
                        .put("transactionId", txnId)
                        .put("success", true))
        ).toJson();
        assertEquals("SUCCESS", result.getString("paymentStatus"));
        assertEquals("SUCCESS", result.getString("orderStatus"));
    }

    // ──────────────────────────────────────────────
    // helper
    // ──────────────────────────────────────────────

    private String confirmAndGetTxnId(String orderId, String idempotencyKey) {
        PaymentConfirmReq req = PaymentConfirmReq.fromJson(new JsonObject()
                .put("orderId", orderId)
                .put("expectedPayAmount", 10.0)
                .put("paymentMethod", 0)
                .put("idempotencyKey", idempotencyKey));
        return paymentService.confirm(req).toJson().getString("transactionId");
    }
}