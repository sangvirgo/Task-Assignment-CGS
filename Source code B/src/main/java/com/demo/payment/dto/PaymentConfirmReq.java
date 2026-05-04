package com.demo.payment.dto;

import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;

public class PaymentConfirmReq {
    private String orderId;
    private BigDecimal expectedPayAmount;
    private Integer paymentMethod;
    private String idempotencyKey;

    public static PaymentConfirmReq fromJson(JsonObject json) {
        PaymentConfirmReq req = new PaymentConfirmReq();
        req.orderId = json.getString("orderId");
        req.expectedPayAmount = toBigDecimal(json.getValue("expectedPayAmount"));
        req.paymentMethod = json.getInteger("paymentMethod");
        req.idempotencyKey = json.getString("idempotencyKey");
        return req;
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    public String getOrderId() {
        return orderId;
    }

    public BigDecimal getExpectedPayAmount() {
        return expectedPayAmount;
    }

    public Integer getPaymentMethod() {
        return paymentMethod;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
