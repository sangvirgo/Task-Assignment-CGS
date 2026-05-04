package com.demo.packing.dto;

import java.math.BigDecimal;
import java.util.List;

public class DefaultItemRes {
    private BigDecimal totalPrice;
    private List<ItemInfoRes> ItemInfoList;

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public DefaultItemRes setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
        return this;
    }

    public List<ItemInfoRes> getItemInfoList() {
        return ItemInfoList;
    }

    public DefaultItemRes setItemInfoList(List<ItemInfoRes> rackInfoList) {
        this.ItemInfoList = rackInfoList;
        return this;
    }
}
