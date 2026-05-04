package com.demo.packing.dto;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class PreBookingItemReq {
    private String itemId;
    private List<String> subItemIdList;

    public static PreBookingItemReq fromJson(JsonObject json) {
        PreBookingItemReq req = new PreBookingItemReq();
        req.itemId = json.getString("itemDesignId");
        if (req.itemId == null || req.itemId.isBlank()) {
            req.itemId = json.getString("rackDesignId");
        }

        JsonArray spaces = json.getJsonArray("spaceIdList");
        List<String> parsed = new ArrayList<>();
        if (spaces != null) {
            for (int i = 0; i < spaces.size(); i++) {
                String value = spaces.getString(i);
                if (value != null) {
                    parsed.add(value);
                }
            }
        }
        req.subItemIdList = parsed;
        return req;
    }

    public String getItemId() {
        return itemId;
    }

    public List<String> getSubItemIdList() {
        return subItemIdList;
    }
}
