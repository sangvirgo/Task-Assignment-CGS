package com.demo.payment.dto;

import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;

public class PaymentPreviewRes {
    private String orderId;
    private BigDecimal subTotal;
    private BigDecimal shippingFee;
    private BigDecimal voucherDiscount;
    private BigDecimal walletUsed;
    private BigDecimal payAmount;
    private Integer paymentMethod;
    private String currency;

    public PaymentPreviewRes setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public PaymentPreviewRes setSubTotal(BigDecimal subTotal) {
        this.subTotal = subTotal;
        return this;
    }

    public PaymentPreviewRes setShippingFee(BigDecimal shippingFee) {
        this.shippingFee = shippingFee;
        return this;
    }

    public PaymentPreviewRes setVoucherDiscount(BigDecimal voucherDiscount) {
        this.voucherDiscount = voucherDiscount;
        return this;
    }

    public PaymentPreviewRes setWalletUsed(BigDecimal walletUsed) {
        this.walletUsed = walletUsed;
        return this;
    }

    public PaymentPreviewRes setPayAmount(BigDecimal payAmount) {
        this.payAmount = payAmount;
        return this;
    }

    public PaymentPreviewRes setPaymentMethod(Integer paymentMethod) {
        this.paymentMethod = paymentMethod;
        return this;
    }

    public PaymentPreviewRes setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("orderId", orderId)
                .put("subTotal", subTotal)
                .put("shippingFee", shippingFee)
                .put("voucherDiscount", voucherDiscount)
                .put("walletUsed", walletUsed)
                .put("payAmount", payAmount)
                .put("paymentMethod", paymentMethod)
                .put("currency", currency);
    }
}
