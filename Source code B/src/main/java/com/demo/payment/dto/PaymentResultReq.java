package com.demo.payment.dto;

import io.vertx.core.json.JsonObject;

public class PaymentResultReq {
    private String transactionId;
    private Boolean success;
    private String reason;

    public static PaymentResultReq fromJson(JsonObject json) {
        PaymentResultReq req = new PaymentResultReq();
        req.transactionId = json.getString("transactionId");
        req.success = json.getBoolean("success");
        req.reason = json.getString("reason");
        return req;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Boolean getSuccess() {
        return success;
    }

    public String getReason() {
        return reason;
    }
}
