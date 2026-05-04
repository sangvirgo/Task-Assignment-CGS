package com.demo.packing.handler;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class HealthHandler {
    public static void healthHandler(RoutingContext ctx) {
        ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                        .put("status", "UP")
                        .put("service", "package-api-service")
                        .encode());
    }
}
