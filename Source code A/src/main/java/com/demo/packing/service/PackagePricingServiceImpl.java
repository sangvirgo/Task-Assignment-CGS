package com.demo.packing.service;

import com.demo.packing.dto.CustomizeItemRes;
import com.demo.packing.dto.DefaultItemRes;
import com.demo.packing.dto.ItemDetailsRes;
import com.demo.packing.dto.ItemInfoRes;
import com.demo.packing.dto.PreBookingItemReq;
import com.demo.packing.dto.PreBookingPackageReq;
import com.demo.packing.dto.PriceSummaryRes;
import com.demo.packing.record.DiscountOnFullPaymentRecord;
import com.demo.packing.record.DiscountOnRentalPeriodRecord;
import com.demo.packing.record.PackageRecord;
import com.demo.packing.util.DateTimeUtil;
import com.demo.packing.util.PaymentOption;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Collections;

public class PackagePricingServiceImpl implements IPackagePricingService {
        private final IItemRevenueConfigRepo rackRevenueConfigRepo = new ItemRevenueConfigRepoImpl();

        @Override
        public PackageRecord getPackageById(String packageId) {
                return new PackageRecord(
                                packageId,
                                "config123",
                                "Standard Package",
                                1,
                                10,
                                "US",
                                "NYC",
                                "Manhattan",
                                "A",
                                "Venue A",
                                BigDecimal.valueOf(100.00),
                                BigDecimal.valueOf(80.00),
                                5,
                                "This is a standard package.",
                                "Terms and conditions apply.",
                                "image_url",
                                1,
                                1,
                                null,
                                LocalDateTime.now(),
                                "admin",
                                LocalDateTime.now(),
                                "admin");
        }

        @Override
        public boolean checkAvailablePackage(LocalDateTime fromDate, LocalDateTime toDate,
                        PackageRecord packageRecord) {
                return true;
        }

        @Override
        public ItemDetailsRes getItemDetailsById(String itemId, String companyId) {
                return new ItemDetailsRes()
                        .setId(itemId)
                        .setStatus(1)
                        .setPrice(BigDecimal.valueOf(25.00));
        }

        @Override
        public PriceSummaryRes calculateBookingPackagePrice(
                        PreBookingPackageReq data,
                        PackageRecord packageRecord,
                        Integer paymentOption,
                        List<ItemDetailsRes> itemDetailsList) {
                LocalDateTime toDate = DateTimeUtil.getToDate(data.getFromDate(), data.getPeriod(),
                                data.getPeriodValue());
                List<ItemInfoRes> defaultItemList = new ArrayList<>();
                List<ItemInfoRes> customizeItemList = new ArrayList<>();

                List<DiscountOnRentalPeriodRecord> rawRentalPeriodList = rackRevenueConfigRepo
                .getListDiscountOnRentalPeriodByConfigurationId(packageRecord.getItemRevenueConfigId());

                List<DiscountOnRentalPeriodRecord> base = rawRentalPeriodList != null 
                ? rawRentalPeriodList 
                : Collections.<DiscountOnRentalPeriodRecord>emptyList();  

                List<DiscountOnRentalPeriodRecord> discountOnRentalPeriodList = base.stream()
                .sorted((a, b) -> b.getRentalPeriod() - a.getRentalPeriod())
                .collect(Collectors.toList());

                List<DiscountOnFullPaymentRecord> rawFullPaymentList = rackRevenueConfigRepo
                .getListDiscountOnFullPaymentByConfigurationId(packageRecord.getItemRevenueConfigId());

                List<DiscountOnFullPaymentRecord> baseFullPayment = rawFullPaymentList != null
                ? rawFullPaymentList
                : Collections.<DiscountOnFullPaymentRecord>emptyList();  // thêm explicit type ở đây

                List<DiscountOnFullPaymentRecord> discountOnFullPaymentList = baseFullPayment.stream()
                .sorted((a, b) -> b.getRentalPeriod() - a.getRentalPeriod())
                .collect(Collectors.toList());


                BigDecimal packagePrice = packageRecord.getOfficialPrice();
                BigDecimal packageTotalPrice = packageRecord.getOfficialPrice()
                                .multiply(BigDecimal.valueOf(data.getPeriodValue()));
                BigDecimal subTotal = packageTotalPrice;

                Double discountOnFullPayment = calculateDiscountOnFullPayment(data.getPeriodValue(),
                                discountOnFullPaymentList);
                Double discountOnRentalPeriod = calculateDiscountOnRentalPeriod(data.getPeriodValue(),
                                discountOnRentalPeriodList);
                BigDecimal fullPaymentOptionDiscount = packageTotalPrice.multiply(
                                BigDecimal.valueOf(discountOnFullPayment + discountOnRentalPeriod)
                                                .divide(BigDecimal.valueOf(100)))
                                .setScale(2, RoundingMode.HALF_UP);
                BigDecimal payMonthlyOptionDiscount = packageTotalPrice
                                .multiply(BigDecimal.valueOf(discountOnRentalPeriod))
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                BigDecimal packageFullPaymentPrice = packageTotalPrice.subtract(fullPaymentOptionDiscount)
                                .setScale(2, RoundingMode.HALF_UP);
                BigDecimal packagePayMonthlyPrice = packageTotalPrice.subtract(payMonthlyOptionDiscount).setScale(2,
                                RoundingMode.HALF_UP);
                BigDecimal payMonthlyPrice = packagePayMonthlyPrice
                                .divide(BigDecimal.valueOf(data.getPeriodValue()), RoundingMode.HALF_UP)
                                .setScale(2, RoundingMode.HALF_UP);

                BigDecimal packageFullPaymentTotalPrice = packageFullPaymentPrice;
                BigDecimal packagePayMonthlyTotalPrice = packagePayMonthlyPrice;

                BigDecimal tax = BigDecimal.ZERO;

                PriceSummaryRes priceSummaryRes = new PriceSummaryRes()
                                .setPackageId(packageRecord.getPackageId())
                                .setPackageName(packageRecord.getPackageName())
                                .setFromDate(data.getFromDate())
                                .setToDate(DateTimeUtil.getLocalDateTime(toDate))
                                .setRentalPeriodValue(data.getPeriodValue())
                                .setTaxPercent(0.0);

                if (data.getList() != null && !data.getList().isEmpty()) {
                        BigDecimal totalCustomItemPrice = BigDecimal.ZERO;
                        Map<String, Integer> mapNumberSubItemOfItem = data.getList().stream()
                                        .collect(Collectors.toMap(PreBookingItemReq::getItemId,
                                                        r -> r.getSubItemIdList().size()));

                        for (ItemDetailsRes item : itemDetailsList) {
                                ItemInfoRes itemInfoRes = new ItemInfoRes();
                                itemInfoRes.setItemId(item.getId())
                                                .setNumberOfItems(mapNumberSubItemOfItem.get(item.getId()))
                                                .setItemName(item.getItemName())
                                                .setPrice(item.getPrice()
                                                                .multiply(BigDecimal.valueOf(mapNumberSubItemOfItem
                                                                                .get(item.getId()))));

                                if (itemInfoRes.getPrice().compareTo(BigDecimal.ZERO) == 0) {
                                        defaultItemList.add(itemInfoRes);
                                } else {
                                        totalCustomItemPrice = totalCustomItemPrice
                                                        .add(itemInfoRes.getPrice());
                                        customizeItemList.add(itemInfoRes);
                                }
                        }

                        packageFullPaymentPrice = packageFullPaymentPrice.add(totalCustomItemPrice);
                        packagePayMonthlyPrice = packagePayMonthlyPrice.add(totalCustomItemPrice);
                        payMonthlyPrice = payMonthlyPrice.add(
                                        totalCustomItemPrice.divide(BigDecimal.valueOf(data.getPeriodValue()),
                                                        RoundingMode.HALF_UP));
                        subTotal = subTotal.add(totalCustomItemPrice);

                        priceSummaryRes
                                        .setDefaultItem(new DefaultItemRes()
                                                        .setItemInfoList(defaultItemList)
                                                        .setTotalPrice(BigDecimal.ZERO))
                                        .setCustomizeItem(new CustomizeItemRes()
                                                        .setItemInfoList(customizeItemList)
                                                        .setTotalPrice(totalCustomItemPrice))
                                        .setTax(tax);
                }

                if (paymentOption != null) {
                        if (paymentOption.equals(PaymentOption.FULLPAYMENT.getCode())) {
                                priceSummaryRes
                                                .setPackagePrice(packagePrice)
                                                .setPackageTotalPrice(packageFullPaymentTotalPrice)
                                                .setTotalDiscount(fullPaymentOptionDiscount)
                                                .setTax(tax)
                                                .setTaxPercent(0.0)
                                                .setSubTotal(tax.add(packageFullPaymentPrice))
                                                .setContractTotalPrice(tax.add(packageFullPaymentPrice));
                        } else if (paymentOption.equals(PaymentOption.PAYMONTHLY.getCode())) {
                                priceSummaryRes
                                                .setPackagePrice(packagePrice)
                                                .setPackageTotalPrice(packagePayMonthlyTotalPrice)
                                                .setPayMonthlyPrice(payMonthlyPrice)
                                                .setTotalDiscount(payMonthlyOptionDiscount)
                                                .setTax(tax)
                                                .setTaxPercent(0.0)
                                                .setSubTotal(tax.add(packagePayMonthlyPrice))
                                                .setContractTotalPrice(tax.add(packagePayMonthlyPrice));
                        }
                } else {
                        priceSummaryRes.setPackagePrice(packagePrice)
                                        .setPackageTotalPrice(packageTotalPrice)
                                        .setSubTotal(subTotal)
                                        .setPackageFullPaymentPrice(packageFullPaymentPrice)
                                        .setFullPaymentOptionDiscount(fullPaymentOptionDiscount)
                                        .setPackagePayMonthlyPrice(packagePayMonthlyPrice)
                                        .setPayMonthlyOptionDiscount(payMonthlyOptionDiscount)
                                        .setPayMonthlyPrice(payMonthlyPrice);
                }

                return priceSummaryRes;
        }

        private Double calculateDiscountOnFullPayment(Integer periodValue,
                        List<DiscountOnFullPaymentRecord> discountOnFullPaymentList) {
                for (DiscountOnFullPaymentRecord item : discountOnFullPaymentList) {
                        if (periodValue >= item.getRentalPeriod()) {
                                return item.getDiscountPercent();
                        }
                }
                return (double) 0;
        }

        private Double calculateDiscountOnRentalPeriod(Integer periodValue,
                        List<DiscountOnRentalPeriodRecord> discountOnRentalPeriodList) {
                for (DiscountOnRentalPeriodRecord item : discountOnRentalPeriodList) {
                        if (periodValue >= item.getRentalPeriod()) {
                                return item.getDiscountPercent();
                        }
                }
                return (double) 0;
        }
}