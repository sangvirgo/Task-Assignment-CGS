package com.demo.packing.handler;

import com.demo.packing.dto.ItemDetailsRes;
import com.demo.packing.dto.PreBookingItemReq;
import com.demo.packing.dto.PreBookingPackageReq;
import com.demo.packing.dto.PriceSummaryRes;
import com.demo.packing.record.PackageRecord;
import com.demo.packing.service.PackagePricingServiceImpl;
import com.demo.packing.service.IPackagePricingService;
import com.demo.packing.util.DateTimeUtil;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PackageHandler {

    private static final int APPROVED_STATUS = 1;
    private static final IPackagePricingService packagePricingService = new PackagePricingServiceImpl();

    private PackageHandler() {
    }

    public static void calculatePackagePriceHandler(RoutingContext context) {
        try {
            JsonObject requestBody = context.body().asJsonObject();
            if (requestBody == null) {
                throw new IllegalArgumentException("Request body is required");
            }

            PreBookingPackageReq data = PreBookingPackageReq.fromJson(requestBody);
            validateRequest(data);

            LocalDateTime fromDate = DateTimeUtil.toLocalDateTime(data.getFromDate());
            LocalDateTime toDate = DateTimeUtil.getToDate(data.getFromDate(), data.getPeriod(), data.getPeriodValue());
            PackageRecord packageRecord = packagePricingService.getPackageById(data.getPackageId());

            if (!packagePricingService.checkAvailablePackage(fromDate, toDate, packageRecord)) {
                context.response()
                        .putHeader("Content-Type", "application/json")
                        .setStatusCode(409)
                        .end(new JsonObject()
                                .put("code", "UNAVAILABLE_PACKAGE")
                                .put("message", "Package is unavailable for the selected period")
                                .encode());
                return;
            }

            List<ItemDetailsRes> itemDetailsList = new ArrayList<>();
            if (data.getList() != null && !data.getList().isEmpty()) {
                String companyId = getCompanyIdFromClaims(context);
                for (PreBookingItemReq preBookingItemReq : data.getList()) {
                    ItemDetailsRes itemDetails = packagePricingService.getItemDetailsById(preBookingItemReq.getItemId(), companyId);
                    if (itemDetails.getStatus() == null || itemDetails.getStatus() != APPROVED_STATUS) {
                        throw new IllegalArgumentException("Item is not approved");
                    }
                    itemDetailsList.add(itemDetails);
                }
            }

            PriceSummaryRes priceSummaryRes = packagePricingService.calculateBookingPackagePrice(data, packageRecord, null, itemDetailsList);
            context.response()
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject()
                            .put("message", "Package price calculated")
                            .put("data", priceSummaryRes.toJson())
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
                            .put("message", "Error on calculating the price of booking package")
                            .encode());
        }
    }

    private static void validateRequest(PreBookingPackageReq data) {
        if (data.getFromDate() == null) {
            throw new IllegalArgumentException("fromDate is required");
        }
        if (data.getPeriod() == null) {
            throw new IllegalArgumentException("period is required");
        }
        if (data.getPeriodValue() == null || data.getPeriodValue() <= 0) {
            throw new IllegalArgumentException("periodValue must be greater than 0");
        }
        if (data.getPackageId() == null || data.getPackageId().isBlank()) {
            throw new IllegalArgumentException("packageId is required");
        }
    }

    private static String getCompanyIdFromClaims(RoutingContext context) {
        JsonObject claims = context.get("jwtClaims");
        if (claims == null) {
            return "demo-company";
        }
        String companyId = claims.getString("companyId");
        if (companyId == null || companyId.isBlank()) {
            companyId = claims.getString("companyid", "demo-company");
        }
        return companyId;
    }

}
