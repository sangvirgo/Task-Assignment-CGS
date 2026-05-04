package com.demo.packing.service;

import java.util.List;

import com.demo.packing.record.DiscountOnFullPaymentRecord;
import com.demo.packing.record.DiscountOnRentalPeriodRecord;

public interface IItemRevenueConfigRepo {
    List<DiscountOnRentalPeriodRecord> getListDiscountOnRackNumbersByConfigurationId(String configurationId);
    List<DiscountOnRentalPeriodRecord> getListDiscountOnRentalPeriodByConfigurationId(String configurationId);
    List<DiscountOnFullPaymentRecord> getListDiscountOnFullPaymentByConfigurationId(String configurationId);
}
