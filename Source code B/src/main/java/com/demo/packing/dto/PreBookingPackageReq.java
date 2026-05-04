package com.demo.packing.dto;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class PreBookingPackageReq {
    private Long fromDate;
    private Integer period;
    private Integer periodValue;
    private String packageId;
    private List<PreBookingItemReq> list;

    public static PreBookingPackageReq fromJson(JsonObject json) {
        PreBookingPackageReq req = new PreBookingPackageReq();
        req.fromDate = json.getLong("fromDate");
        req.period = json.getInteger("period");
        req.periodValue = json.getInteger("periodValue");
        req.packageId = json.getString("packageId");

        JsonArray itemList = json.getJsonArray("list");
        List<PreBookingItemReq> parsedList = new ArrayList<>();
        if (itemList != null) {
            for (int i = 0; i < itemList.size(); i++) {
                JsonObject item = itemList.getJsonObject(i);
                if (item != null) {
                    parsedList.add(PreBookingItemReq.fromJson(item));
                }
            }
        }
        req.list = parsedList;
        return req;
    }

    public Long getFromDate() {
        return fromDate;
    }

    public Integer getPeriod() {
        return period;
    }

    public Integer getPeriodValue() {
        return periodValue;
    }

    public String getPackageId() {
        return packageId;
    }

    public List<PreBookingItemReq> getList() {
        return list;
    }
}
