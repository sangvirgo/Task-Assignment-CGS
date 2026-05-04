package com.demo.payment.dto;

import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;

public class PaymentPreviewReq {
    private String orderId;
    private BigDecimal subTotal;
    private BigDecimal shippingFee;
    private String voucherCode;
    private Integer paymentMethod;
    private BigDecimal walletAmount;

    public static PaymentPreviewReq fromJson(JsonObject json) {
        PaymentPreviewReq req = new PaymentPreviewReq();
        req.orderId = json.getString("orderId");
        req.subTotal = toBigDecimal(json.getValue("subTotal"));
        req.shippingFee = toBigDecimal(json.getValue("shippingFee"));
        req.voucherCode = json.getString("voucherCode");
        req.paymentMethod = json.getInteger("paymentMethod");
        req.walletAmount = toBigDecimal(json.getValue("walletAmount"));
        return req;
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
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

    public BigDecimal getSubTotal() {
        return subTotal;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public Integer getPaymentMethod() {
        return paymentMethod;
    }

    public BigDecimal getWalletAmount() {
        return walletAmount;
    }
}
