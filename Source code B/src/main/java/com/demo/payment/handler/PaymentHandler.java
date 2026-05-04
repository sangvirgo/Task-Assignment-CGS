package com.demo.payment.handler;

import com.demo.payment.dto.PaymentConfirmReq;
import com.demo.payment.dto.PaymentConfirmRes;
import com.demo.payment.dto.PaymentPendingRes;
import com.demo.payment.dto.PaymentPreviewReq;
import com.demo.payment.dto.PaymentPreviewRes;
import com.demo.payment.dto.PaymentResultReq;
import com.demo.payment.service.IPaymentService;
import com.demo.payment.service.PaymentServiceImpl;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class PaymentHandler {
    private static final IPaymentService paymentService = new PaymentServiceImpl();

    private PaymentHandler() {
    }

    public static void paymentPreviewHandler(RoutingContext context) {
        try {
            JsonObject requestBody = context.body().asJsonObject();
            if (requestBody == null) {
                throw new IllegalArgumentException("Request body is required");
            }

            PaymentPreviewReq request = PaymentPreviewReq.fromJson(requestBody);
            PaymentPreviewRes response = paymentService.preview(request);

            context.response()
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject()
                            .put("message", "Payment preview calculated")
                            .put("data", response.toJson())
                            .encode());
        } catch (IllegalArgumentException ex) {
            context.response()
                    .putHeader("Content-Type", "application/json")
                    .setStatusCode(400)
                    .end(new JsonObject()
                            .put("code", "BAD_REQUEST")
                            .put("message", ex.getMessage())
                            .encode());
        } catch (Exception ex) {
            context.response()
                    .putHeader("Content-Type", "application/json")
                    .setStatusCode(500)
                    .end(new JsonObject()
                            .put("code", "SERVER_ERROR")
                            .put("message", "Error on payment preview")
                            .encode());
        }
    }

    public static void confirmPaymentHandler(RoutingContext context) {
        try {
            JsonObject requestBody = context.body().asJsonObject();
            if (requestBody == null) {
                throw new IllegalArgumentException("Request body is required");
            }

            PaymentConfirmReq request = PaymentConfirmReq.fromJson(requestBody);
            PaymentConfirmRes response = paymentService.confirm(request);

            context.response()
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject()
                            .put("message", "Payment transaction created")
                            .put("data", response.toJson())
                            .encode());
        } catch (IllegalArgumentException ex) {
            context.response()
                    .putHeader("Content-Type", "application/json")
                    .setStatusCode(400)
                    .end(new JsonObject()
                            .put("code", "BAD_REQUEST")
                            .put("message", ex.getMessage())
                            .encode());
        } catch (Exception ex) {
            context.response()
                    .putHeader("Content-Type", "application/json")
                    .setStatusCode(500)
                    .end(new JsonObject()
                            .put("code", "SERVER_ERROR")
                            .put("message", "Error on confirming payment")
                            .encode());
        }
    }

    public static void pendingPaymentHandler(RoutingContext context) {
        try {
            String transactionId = context.pathParam("transactionId");
            PaymentPendingRes response = paymentService.pending(transactionId);

            context.response()
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject()
                            .put("message", "Payment pending status fetched")
                            .put("data", response.toJson())
                            .encode());
        } catch (IllegalArgumentException ex) {
            int statusCode = "Transaction not found".equals(ex.getMessage()) ? 404 : 400;
            context.response()
                    .putHeader("Content-Type", "application/json")
                    .setStatusCode(statusCode)
                    .end(new JsonObject()
                            .put("code", statusCode == 404 ? "NOT_FOUND" : "BAD_REQUEST")
                            .put("message", ex.getMessage())
                            .encode());
        } catch (Exception ex) {
            context.response()
                    .putHeader("Content-Type", "application/json")
                    .setStatusCode(500)
                    .end(new JsonObject()
                            .put("code", "SERVER_ERROR")
                            .put("message", "Error on getting pending payment status")
                            .encode());
        }
    }

    public static void paymentResultHandler(RoutingContext context) {
        try {
            JsonObject requestBody = context.body().asJsonObject();
            if (requestBody == null) {
                throw new IllegalArgumentException("Request body is required");
            }

            PaymentResultReq request = PaymentResultReq.fromJson(requestBody);
            PaymentPendingRes response = paymentService.applyPaymentResult(request);

            context.response()
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject()
                            .put("message", "Payment result applied")
                            .put("data", response.toJson())
                            .encode());
        } catch (IllegalArgumentException ex) {
            int statusCode = "Transaction not found".equals(ex.getMessage()) ? 404 : 400;
            context.response()
                    .putHeader("Content-Type", "application/json")
                    .setStatusCode(statusCode)
                    .end(new JsonObject()
                            .put("code", statusCode == 404 ? "NOT_FOUND" : "BAD_REQUEST")
                            .put("message", ex.getMessage())
                            .encode());
        } catch (Exception ex) {
            context.response()
                    .putHeader("Content-Type", "application/json")
                    .setStatusCode(500)
                    .end(new JsonObject()
                            .put("code", "SERVER_ERROR")
                            .put("message", "Error on applying payment result")
                            .encode());
        }
    }
}
