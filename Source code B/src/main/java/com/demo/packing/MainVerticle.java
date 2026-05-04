package com.demo.packing;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;

import java.util.Set;

import com.demo.packing.handler.HealthHandler;
import com.demo.packing.handler.PackageHandler;
import com.demo.packing.handler.RouterHandler;
import com.demo.payment.handler.PaymentHandler;

public class MainVerticle extends VerticleBase {

  private static final String BASE_API_PATH = "/api/v1";
  private static final String HEALTH_API = BASE_API_PATH + "/health";
  private static final String PACKAGE_API = BASE_API_PATH + "/package-price";
  private static final String PAYMENT_PREVIEW_API = BASE_API_PATH + "/checkout/payment-preview";
  private static final String PAYMENT_CONFIRM_API = BASE_API_PATH + "/checkout/confirm";
  private static final String PAYMENT_PENDING_API = BASE_API_PATH + "/checkout/pending/:transactionId";
  private static final String PAYMENT_RESULT_API = BASE_API_PATH + "/checkout/payment-result";

  @Override
  public Future<?> start() {
    Router router = Router.router(vertx);

    router.route().handler(BodyHandler.create());
    router.route().handler(
      CorsHandler.create()
        .addOrigin("*")
        .allowedMethods(Set.of(
          io.vertx.core.http.HttpMethod.GET,
          io.vertx.core.http.HttpMethod.POST,
          io.vertx.core.http.HttpMethod.PUT,
          io.vertx.core.http.HttpMethod.DELETE,
          io.vertx.core.http.HttpMethod.OPTIONS
        ))
        .allowedHeader("Content-Type")
        .allowedHeader("Authorization")
    );

    router.get(HEALTH_API).handler(HealthHandler::healthHandler);
    router.post(PACKAGE_API)
      .blockingHandler(RouterHandler::internalDecodeToken)
      .blockingHandler(PackageHandler::calculatePackagePriceHandler);

    router.post(PAYMENT_PREVIEW_API)
      .blockingHandler(PaymentHandler::paymentPreviewHandler);

    router.post(PAYMENT_CONFIRM_API)
      .blockingHandler(PaymentHandler::confirmPaymentHandler);

    router.get(PAYMENT_PENDING_API)
      .blockingHandler(PaymentHandler::pendingPaymentHandler);

    router.post(PAYMENT_RESULT_API)
      .blockingHandler(PaymentHandler::paymentResultHandler);

    int port = Integer.parseInt(System.getProperty("PORT", "8080"));
    return vertx.createHttpServer()
      .requestHandler(router)
      .listen(port)
      .onSuccess(http -> {
        System.out.println("HTTP server started on port " + http.actualPort());
      });
  }
}
