package com.demo.payment.dto;

import io.vertx.core.json.JsonObject;

public class PaymentPendingRes {
    private String transactionId;
    private String paymentStatus;
    private String orderStatus;
    private String failureReason;
    private Long expiredAt;

    public PaymentPendingRes setTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public PaymentPendingRes setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
        return this;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public PaymentPendingRes setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
        return this;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public PaymentPendingRes setFailureReason(String failureReason) {
        this.failureReason = failureReason;
        return this;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public PaymentPendingRes setExpiredAt(Long expiredAt) {
        this.expiredAt = expiredAt;
        return this;
    }

    public Long getExpiredAt() {
        return expiredAt;
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("transactionId", transactionId)
                .put("paymentStatus", paymentStatus)
                .put("orderStatus", orderStatus)
                .put("failureReason", failureReason)
                .put("expiredAt", expiredAt);
    }
}
