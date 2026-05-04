package com.demo.packing.service;

import com.demo.packing.dto.ItemDetailsRes;
import com.demo.packing.dto.PreBookingPackageReq;
import com.demo.packing.dto.PriceSummaryRes;
import com.demo.packing.record.PackageRecord;

import java.time.LocalDateTime;
import java.util.List;

public interface IPackagePricingService {
    PackageRecord getPackageById(String packageId);

    boolean checkAvailablePackage(LocalDateTime fromDate, LocalDateTime toDate, PackageRecord packageRecord);

    ItemDetailsRes getItemDetailsById(String itemDesignId, String companyId);

    PriceSummaryRes calculateBookingPackagePrice(
            PreBookingPackageReq data,
            PackageRecord packageRecord,
            Integer paymentOption,
            List<ItemDetailsRes> itemDetailsList
    );
}
