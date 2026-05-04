package com.demo.packing.dto;

import java.math.BigDecimal;

public class ItemDetailsRes {
    private String id;
    private String itemName;
    private String companyId;
    private BigDecimal price;
    private String design;
    private String extensionDesign;
    private String image;

    private Double width;
    private Double height;
    private Double depth;
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public ItemDetailsRes setStatus(Integer status) {
        this.status = status;
        return this;
    }

    public String getId() {
        return id;
    }

    public ItemDetailsRes setId(String id) {
        this.id = id;
        return this;
    }

    public String getItemName() {
        return itemName;
    }

    public ItemDetailsRes setItemName(String rackDesignName) {
        this.itemName = rackDesignName;
        return this;
    }

    public String getCompanyId() {
        return companyId;
    }

    public ItemDetailsRes setCompanyId(String companyId) {
        this.companyId = companyId;
        return this;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public ItemDetailsRes setPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public String getDesign() {
        return design;
    }

    public ItemDetailsRes setDesign(String design) {
        this.design = design;
        return this;
    }

    public String getExtensionDesign() {
        return extensionDesign;
    }

    public ItemDetailsRes setExtensionDesign(String extensionDesign) {
        this.extensionDesign = extensionDesign;
        return this;
    }

    public String getImage() {
        return image;
    }

    public ItemDetailsRes setImage(String image) {
        this.image = image;
        return this;
    }

    public Double getWidth() {
        return width;
    }

    public ItemDetailsRes setWidth(Double width) {
        this.width = width;
        return this;
    }

    public Double getHeight() {
        return height;
    }

    public ItemDetailsRes setHeight(Double height) {
        this.height = height;
        return this;
    }

    public Double getDepth() {
        return depth;
    }

    public ItemDetailsRes setDepth(Double depth) {
        this.depth = depth;
        return this;
    }
}
