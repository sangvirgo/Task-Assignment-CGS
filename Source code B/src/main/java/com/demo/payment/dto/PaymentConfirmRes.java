package com.demo.payment.dto;

import io.vertx.core.json.JsonObject;

public class PaymentConfirmRes {
    private String transactionId;
    private String paymentStatus;
    private String paymentUrl;
    private Long expiredAt;

    public PaymentConfirmRes setTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public PaymentConfirmRes setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
        return this;
    }

    public PaymentConfirmRes setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
        return this;
    }

    public PaymentConfirmRes setExpiredAt(Long expiredAt) {
        this.expiredAt = expiredAt;
        return this;
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("transactionId", transactionId)
                .put("paymentStatus", paymentStatus)
                .put("paymentUrl", paymentUrl)
                .put("expiredAt", expiredAt);
    }
}
