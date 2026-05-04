package com.demo.packing.dto;

import java.math.BigDecimal;

public class ItemInfoRes {
    private String itemId;
    private String itemName;
    private Integer numberOfItems;
    private BigDecimal price;

    public String getItemId() {
        return itemId;
    }

    public ItemInfoRes setItemId(String rackDesignId) {
        this.itemId = rackDesignId;
        return this;
    }

    public String getItemName() {
        return itemName;
    }

    public ItemInfoRes setItemName(String rackDesignName) {
        this.itemName = rackDesignName;
        return this;
    }

    public Integer getNumberOfItems() {
        return numberOfItems;
    }

    public ItemInfoRes setNumberOfItems(Integer numberOfRacks) {
        this.numberOfItems = numberOfRacks;
        return this;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public ItemInfoRes setPrice(BigDecimal price) {
        this.price = price;
        return this;
    }
}
