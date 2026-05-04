package com.demo.packing.record;

import java.time.LocalDateTime;

public class DiscountOnFullPaymentRecord {
    private final String id;
    private final String rackRevenueConfigId;
    private final Integer rentalPeriod;
    private final Double discountPercent;
    private final Integer isDeleted;
    private final LocalDateTime createdAt;
    private final String createdBy;
    private final LocalDateTime modifiedAt;
    private final String modifiedBy;

    public DiscountOnFullPaymentRecord(
            String id,
            String rackRevenueConfigId,
            Integer rentalPeriod,
            Double discountPercent,
            Integer isDeleted,
            LocalDateTime createdAt,
            String createdBy,
            LocalDateTime modifiedAt,
            String modifiedBy
    ) {
        this.id = id;
        this.rackRevenueConfigId = rackRevenueConfigId;
        this.rentalPeriod = rentalPeriod;
        this.discountPercent = discountPercent;
        this.isDeleted = isDeleted;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.modifiedAt = modifiedAt;
        this.modifiedBy = modifiedBy;
    }

    public String getId() {
        return id;
    }

    public String getRackRevenueConfigId() {
        return rackRevenueConfigId;
    }

    public Integer getRentalPeriod() {
        return rentalPeriod;
    }

    public Double getDiscountPercent() {
        return discountPercent;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getModifiedAt() {
        return modifiedAt;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }
}
