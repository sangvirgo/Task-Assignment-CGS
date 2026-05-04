package com.demo.packing.util;

public enum PaymentOption {
        FULLPAYMENT(0),
        PAYMONTHLY(1);
        private final Integer code;

        PaymentOption(Integer code) {
            this.code = code;
        }

        public Integer getCode() {
            return code;
        }

        public static PaymentOption safeValueOf(Integer code) {
            for (PaymentOption item :
                    values()) {
                if (item.getCode().equals(code))
                    return item;
            }
            throw new IllegalArgumentException("Unexpected code value: " + code);
        }
    } {
    
}
