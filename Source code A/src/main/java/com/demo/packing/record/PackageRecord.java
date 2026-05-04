package com.demo.packing.record;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PackageRecord {
    private final String packageId;
    private final String itemRevenueConfigId;
    private final String packageName;
    private final Integer packageType;
    private final Integer packageQuantity;
    private final String countryCode;
    private final String cityCode;
    private final String districtCode;
    private final String ranking;
    private final String venue;
    private final BigDecimal originalPrice;
    private final BigDecimal officialPrice;
    private final Integer numberOfItems;
    private final String description;
    private final String termAndCondition;
    private final String image;
    private final Integer displayedStatus;
    private final Integer approvalStatus;
    private final String rejectedReason;
    private final LocalDateTime createdAt;
    private final String createdBy;
    private final LocalDateTime modifiedAt;
    private final String modifiedBy;

    public PackageRecord(
            String packageId,
            String rackRevenueConfigId,
            String packageName,
            Integer packageType,
            Integer packageQuantity,
            String countryCode,
            String cityCode,
            String districtCode,
            String ranking,
            String venue,
            BigDecimal originalPrice,
            BigDecimal officialPrice,
            Integer numberOfRacks,
            String description,
            String termAndCondition,
            String image,
            Integer displayedStatus,
            Integer approvalStatus,
            String rejectedReason,
            LocalDateTime createdAt,
            String createdBy,
            LocalDateTime modifiedAt,
            String modifiedBy
    ) {
        this.packageId = packageId;
        this.itemRevenueConfigId = rackRevenueConfigId;
        this.packageName = packageName;
        this.packageType = packageType;
        this.packageQuantity = packageQuantity;
        this.countryCode = countryCode;
        this.cityCode = cityCode;
        this.districtCode = districtCode;
        this.ranking = ranking;
        this.venue = venue;
        this.originalPrice = originalPrice;
        this.officialPrice = officialPrice;
        this.numberOfItems = numberOfRacks;
        this.description = description;
        this.termAndCondition = termAndCondition;
        this.image = image;
        this.displayedStatus = displayedStatus;
        this.approvalStatus = approvalStatus;
        this.rejectedReason = rejectedReason;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.modifiedAt = modifiedAt;
        this.modifiedBy = modifiedBy;
    }

    public String getPackageId() {
        return packageId;
    }

    public String getItemRevenueConfigId() {
        return itemRevenueConfigId;
    }

    public String getPackageName() {
        return packageName;
    }

    public Integer getPackageType() {
        return packageType;
    }

    public Integer getPackageQuantity() {
        return packageQuantity;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getCityCode() {
        return cityCode;
    }

    public String getDistrictCode() {
        return districtCode;
    }

    public String getRanking() {
        return ranking;
    }

    public String getVenue() {
        return venue;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public BigDecimal getOfficialPrice() {
        return officialPrice;
    }

    public Integer getNumberOfItems() {
        return numberOfItems;
    }

    public String getDescription() {
        return description;
    }

    public String getTermAndCondition() {
        return termAndCondition;
    }

    public String getImage() {
        return image;
    }

    public Integer getDisplayedStatus() {
        return displayedStatus;
    }

    public Integer getApprovalStatus() {
        return approvalStatus;
    }

    public String getRejectedReason() {
        return rejectedReason;
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
