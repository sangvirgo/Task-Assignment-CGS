package com.demo.packing.dto;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;
import java.util.List;

public class CustomizeItemRes {
    private BigDecimal totalPrice;
    private List<ItemInfoRes> ItemInfoList;

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public CustomizeItemRes setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
        return this;
    }

    public List<ItemInfoRes> getItemInfoList() {
        return ItemInfoList;
    }

    public CustomizeItemRes setItemInfoList(List<ItemInfoRes> rackInfoList) {
        this.ItemInfoList = rackInfoList;
        return this;
    }

    public JsonObject toJson() {
        JsonArray itemInfoJsonArray = new JsonArray();
        if (ItemInfoList != null) {
            for (ItemInfoRes item : ItemInfoList) {
                itemInfoJsonArray.add(new JsonObject()
                        .put("itemId", item.getItemId())
                        .put("itemName", item.getItemName())
                        .put("numberOfItems", item.getNumberOfItems())
                        .put("price", item.getPrice()));
            }
        }

        return new JsonObject()
                .put("totalPrice", totalPrice)
                .put("itemInfoList", itemInfoJsonArray);
    }
}
