package com.demo.payment.util;

public enum PaymentMethod {
    ABA(0),
    CASH_ON_DELIVERY(1);

    private final int code;

    PaymentMethod(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static PaymentMethod fromCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("paymentMethod is required");
        }
        for (PaymentMethod value : PaymentMethod.values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unsupported paymentMethod");
    }
}
