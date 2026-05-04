package com.demo.payment.service;

import com.demo.payment.dto.PaymentConfirmReq;
import com.demo.payment.dto.PaymentConfirmRes;
import com.demo.payment.dto.PaymentPendingRes;
import com.demo.payment.dto.PaymentResultReq;
import com.demo.payment.dto.PaymentPreviewReq;
import com.demo.payment.dto.PaymentPreviewRes;
import com.demo.payment.util.PaymentMethod;
import java.util.concurrent.ConcurrentHashMap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class PaymentServiceImpl implements IPaymentService {
    private static final String DEFAULT_CURRENCY = "USD";
    private static final Map<String, BigDecimal> VOUCHER_PERCENT_MAP = Map.of(
            "SAVE10", BigDecimal.TEN,
            "SAVE5", BigDecimal.valueOf(5)
    );
    private static final Map<String, PaymentPendingRes> PENDING_TRANSACTION_STORE = new ConcurrentHashMap<>();

    @Override
    public PaymentPreviewRes preview(PaymentPreviewReq request) {
        validatePreviewRequest(request);

        PaymentMethod paymentMethod = PaymentMethod.fromCode(request.getPaymentMethod());

        BigDecimal subTotal = request.getSubTotal().setScale(2, RoundingMode.HALF_UP);
        BigDecimal shippingFee = request.getShippingFee().setScale(2, RoundingMode.HALF_UP);
        BigDecimal voucherDiscount = resolveVoucherDiscount(subTotal, request.getVoucherCode());

        BigDecimal grossAmount = subTotal.add(shippingFee).subtract(voucherDiscount);
        if (grossAmount.compareTo(BigDecimal.ZERO) < 0) {
            grossAmount = BigDecimal.ZERO;
        }

        BigDecimal walletAmount = normalizeMoney(request.getWalletAmount());
        BigDecimal walletUsed = walletAmount.min(grossAmount);
        BigDecimal payAmount = grossAmount.subtract(walletUsed).setScale(2, RoundingMode.HALF_UP);

        return new PaymentPreviewRes()
                .setOrderId(request.getOrderId())
                .setSubTotal(subTotal)
                .setShippingFee(shippingFee)
                .setVoucherDiscount(voucherDiscount)
                .setWalletUsed(walletUsed)
                .setPayAmount(payAmount)
                .setPaymentMethod(paymentMethod.getCode())
                .setCurrency(DEFAULT_CURRENCY);
    }

    @Override
    public PaymentConfirmRes confirm(PaymentConfirmReq request) {
        validateConfirmRequest(request);

        PaymentMethod paymentMethod = PaymentMethod.fromCode(request.getPaymentMethod());
        String transactionId = "TXN" + System.currentTimeMillis();
        Long expiredAt = Instant.now().plusSeconds(15 * 60).toEpochMilli();

        String paymentUrl = null;
        if (paymentMethod == PaymentMethod.ABA) {
            paymentUrl = "https://sandbox.pay.mock/checkout/" + transactionId;
        }

        PaymentConfirmRes response = new PaymentConfirmRes()
                .setTransactionId(transactionId)
                .setPaymentStatus("PENDING")
                .setPaymentUrl(paymentUrl)
                .setExpiredAt(expiredAt);

        PENDING_TRANSACTION_STORE.put(transactionId, new PaymentPendingRes()
                .setTransactionId(transactionId)
                .setPaymentStatus("PENDING")
                .setOrderStatus("PENDING")
                .setExpiredAt(expiredAt));

        return response;
    }

    @Override
    public PaymentPendingRes pending(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("transactionId is required");
        }
        PaymentPendingRes pending = PENDING_TRANSACTION_STORE.get(transactionId);
        if (pending == null) {
            throw new IllegalArgumentException("Transaction not found");
        }
        return pending;
    }

    @Override
    public PaymentPendingRes applyPaymentResult(PaymentResultReq request) {
        validatePaymentResultRequest(request);

        PaymentPendingRes existing = PENDING_TRANSACTION_STORE.get(request.getTransactionId());
        if (existing == null) {
            throw new IllegalArgumentException("Transaction not found");
        }

        PaymentPendingRes updated;
        if (Boolean.TRUE.equals(request.getSuccess())) {
            updated = new PaymentPendingRes()
                    .setTransactionId(request.getTransactionId())
                    .setPaymentStatus("SUCCESS")
                    .setOrderStatus("SUCCESS")
                    .setExpiredAt(existing.getExpiredAt());
        } else {
            updated = new PaymentPendingRes()
                    .setTransactionId(request.getTransactionId())
                    .setPaymentStatus("FAILED")
                    .setOrderStatus("PENDING")
                    .setFailureReason(request.getReason())
                    .setExpiredAt(existing.getExpiredAt());
        }

        PENDING_TRANSACTION_STORE.put(request.getTransactionId(), updated);
        return updated;
    }

    private void validatePreviewRequest(PaymentPreviewReq request) {
        if (request.getOrderId() == null || request.getOrderId().isBlank()) {
            throw new IllegalArgumentException("orderId is required");
        }
        if (request.getSubTotal() == null) {
            throw new IllegalArgumentException("subTotal is required");
        }
        if (request.getShippingFee() == null) {
            throw new IllegalArgumentException("shippingFee is required");
        }
        if (request.getSubTotal().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("subTotal must be greater than or equal to 0");
        }
        if (request.getShippingFee().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("shippingFee must be greater than or equal to 0");
        }
        if (request.getWalletAmount() != null && request.getWalletAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("walletAmount must be greater than or equal to 0");
        }
        PaymentMethod.fromCode(request.getPaymentMethod());
    }

    private void validateConfirmRequest(PaymentConfirmReq request) {
        if (request.getOrderId() == null || request.getOrderId().isBlank()) {
            throw new IllegalArgumentException("orderId is required");
        }
        if (request.getExpectedPayAmount() == null) {
            throw new IllegalArgumentException("expectedPayAmount is required");
        }
        if (request.getExpectedPayAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("expectedPayAmount must be greater than or equal to 0");
        }
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
        PaymentMethod.fromCode(request.getPaymentMethod());
    }

    private BigDecimal resolveVoucherDiscount(BigDecimal subTotal, String voucherCode) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal percent = VOUCHER_PERCENT_MAP.get(voucherCode.toUpperCase());
        if (percent == null) {
            throw new IllegalArgumentException("Invalid voucherCode");
        }

        return subTotal.multiply(percent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private void validatePaymentResultRequest(PaymentResultReq request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.getTransactionId() == null || request.getTransactionId().isBlank()) {
            throw new IllegalArgumentException("transactionId is required");
        }
        if (request.getSuccess() == null) {
            throw new IllegalArgumentException("success is required");
        }
        if (!request.getSuccess() && (request.getReason() == null || request.getReason().isBlank())) {
            throw new IllegalArgumentException("reason is required when payment fails");
        }
    }
}
