package com.demo.packing.dto;

import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;

public class PriceSummaryRes {
    private String packageId;
    private String packageName;
    private Long fromDate;
    private Long toDate;
    private Integer paymentOption;
    private Integer rentalPeriodValue;
    private BigDecimal packagePrice;
    private BigDecimal packageTotalPrice;
    private DefaultItemRes defaultItem;
    private CustomizeItemRes customizeItem;
    private BigDecimal subTotal;
    private BigDecimal packagePayMonthlyPrice;
    private BigDecimal payMonthlyOptionDiscount;
    private BigDecimal payMonthlyPrice;
    private BigDecimal packageFullPaymentPrice;
    private BigDecimal fullPaymentOptionDiscount;
    private Double taxPercent;
    private BigDecimal tax;
    private Double discountPercent;
    private BigDecimal totalDiscount;
    private BigDecimal contractTotalPrice;

    public String getPackageId() {
        return packageId;
    }

    public PriceSummaryRes setPackageId(String packageId) {
        this.packageId = packageId;
        return this;
    }

    public String getPackageName() {
        return packageName;
    }

    public PriceSummaryRes setPackageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    public Long getFromDate() {
        return fromDate;
    }

    public PriceSummaryRes setFromDate(Long fromDate) {
        this.fromDate = fromDate;
        return this;
    }

    public Long getToDate() {
        return toDate;
    }

    public PriceSummaryRes setToDate(Long toDate) {
        this.toDate = toDate;
        return this;
    }

    public Integer getPaymentOption() {
        return paymentOption;
    }

    public PriceSummaryRes setPaymentOption(Integer paymentOption) {
        this.paymentOption = paymentOption;
        return this;
    }

    public Integer getRentalPeriodValue() {
        return rentalPeriodValue;
    }

    public PriceSummaryRes setRentalPeriodValue(Integer rentalPeriodValue) {
        this.rentalPeriodValue = rentalPeriodValue;
        return this;
    }

    public BigDecimal getPackagePrice() {
        return packagePrice;
    }

    public PriceSummaryRes setPackagePrice(BigDecimal packagePrice) {
        this.packagePrice = packagePrice;
        return this;
    }

    public BigDecimal getPackageTotalPrice() {
        return packageTotalPrice;
    }

    public PriceSummaryRes setPackageTotalPrice(BigDecimal packageTotalPrice) {
        this.packageTotalPrice = packageTotalPrice;
        return this;
    }

    public DefaultItemRes getDefaultItem() {
        return defaultItem;
    }

    public PriceSummaryRes setDefaultItem(DefaultItemRes defaultPlanogram) {
        this.defaultItem = defaultPlanogram;
        return this;
    }

    public CustomizeItemRes getCustomizeItem() {
        return customizeItem;
    }

    public PriceSummaryRes setCustomizeItem(CustomizeItemRes customizePlanogram) {
        this.customizeItem = customizePlanogram;
        return this;
    }

    public BigDecimal getSubTotal() {
        return subTotal;
    }

    public PriceSummaryRes setSubTotal(BigDecimal subTotal) {
        this.subTotal = subTotal;
        return this;
    }

    public BigDecimal getPackagePayMonthlyPrice() {
        return packagePayMonthlyPrice;
    }

    public PriceSummaryRes setPackagePayMonthlyPrice(BigDecimal packagePayMonthlyPrice) {
        this.packagePayMonthlyPrice = packagePayMonthlyPrice;
        return this;
    }

    public BigDecimal getPayMonthlyOptionDiscount() {
        return payMonthlyOptionDiscount;
    }

    public PriceSummaryRes setPayMonthlyOptionDiscount(BigDecimal payMonthlyOptionDiscount) {
        this.payMonthlyOptionDiscount = payMonthlyOptionDiscount;
        return this;
    }

    public BigDecimal getPayMonthlyPrice() {
        return payMonthlyPrice;
    }

    public PriceSummaryRes setPayMonthlyPrice(BigDecimal payMonthlyPrice) {
        this.payMonthlyPrice = payMonthlyPrice;
        return this;
    }

    public BigDecimal getPackageFullPaymentPrice() {
        return packageFullPaymentPrice;
    }

    public PriceSummaryRes setPackageFullPaymentPrice(BigDecimal packageFullPaymentPrice) {
        this.packageFullPaymentPrice = packageFullPaymentPrice;
        return this;
    }

    public BigDecimal getFullPaymentOptionDiscount() {
        return fullPaymentOptionDiscount;
    }

    public PriceSummaryRes setFullPaymentOptionDiscount(BigDecimal fullPaymentOptionDiscount) {
        this.fullPaymentOptionDiscount = fullPaymentOptionDiscount;
        return this;
    }

    public Double getTaxPercent() {
        return taxPercent;
    }

    public PriceSummaryRes setTaxPercent(Double taxPercent) {
        this.taxPercent = taxPercent;
        return this;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public PriceSummaryRes setTax(BigDecimal tax) {
        this.tax = tax;
        return this;
    }

    public Double getDiscountPercent() {
        return discountPercent;
    }

    public PriceSummaryRes setDiscountPercent(Double discountPercent) {
        this.discountPercent = discountPercent;
        return this;
    }

    public BigDecimal getTotalDiscount() {
        return totalDiscount;
    }

    public PriceSummaryRes setTotalDiscount(BigDecimal totalDiscount) {
        this.totalDiscount = totalDiscount;
        return this;
    }

    public BigDecimal getContractTotalPrice() {
        return contractTotalPrice;
    }

    public PriceSummaryRes setContractTotalPrice(BigDecimal contractTotalPrice) {
        this.contractTotalPrice = contractTotalPrice;
        return this;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.put("packageId", packageId);
        json.put("packageName", packageName);
        json.put("fromDate", fromDate);
        json.put("toDate", toDate);
        json.put("paymentOption", paymentOption);
        json.put("rentalPeriodValue", rentalPeriodValue);
        json.put("packagePrice", packagePrice);
        json.put("packageTotalPrice", packageTotalPrice);
        if (defaultItem != null) {
            json.put("defaultPlanogram", defaultItem.toJson());
        }
        if (customizeItem != null) {
            json.put("customizePlanogram", customizeItem.toJson());
        }
        json.put("subTotal", subTotal);
        json.put("packagePayMonthlyPrice", packagePayMonthlyPrice);
        json.put("payMonthlyOptionDiscount", payMonthlyOptionDiscount);
        json.put("payMonthlyPrice", payMonthlyPrice);
        json.put("packageFullPaymentPrice", packageFullPaymentPrice);
        json.put("fullPaymentOptionDiscount", fullPaymentOptionDiscount);
        json.put("taxPercent", taxPercent);
        json.put("tax", tax);
        json.put("discountPercent", discountPercent);
        json.put("totalDiscount", totalDiscount);
        json.put("contractTotalPrice", contractTotalPrice);
        return json;
    }
}
