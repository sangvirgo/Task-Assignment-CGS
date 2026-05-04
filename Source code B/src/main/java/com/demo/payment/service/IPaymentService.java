package com.demo.payment.service;

import com.demo.payment.dto.PaymentConfirmReq;
import com.demo.payment.dto.PaymentConfirmRes;
import com.demo.payment.dto.PaymentPendingRes;
import com.demo.payment.dto.PaymentResultReq;
import com.demo.payment.dto.PaymentPreviewReq;
import com.demo.payment.dto.PaymentPreviewRes;

public interface IPaymentService {
    PaymentPreviewRes preview(PaymentPreviewReq request);

    PaymentConfirmRes confirm(PaymentConfirmReq request);

    PaymentPendingRes pending(String transactionId);

    PaymentPendingRes applyPaymentResult(PaymentResultReq request);
}
