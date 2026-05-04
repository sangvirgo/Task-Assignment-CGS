package com.demo.packing.service;

import java.util.List;

import com.demo.packing.record.DiscountOnFullPaymentRecord;
import com.demo.packing.record.DiscountOnRentalPeriodRecord;

public class ItemRevenueConfigRepoImpl implements IItemRevenueConfigRepo {
    @Override
    public List<DiscountOnRentalPeriodRecord> getListDiscountOnRackNumbersByConfigurationId(String configurationId) {
        // This is a mock implementation.
        // This function should query the database to get the list of discounts (% discount, e.g: 30% off) based on the configuration ID.

        return null;
    }

    @Override
    public List<DiscountOnRentalPeriodRecord> getListDiscountOnRentalPeriodByConfigurationId(String configurationId) {
        // This is a mock implementation.
        // This function should query the database to get the list of discounts (% discount, e.g: 30% off) based on the configuration ID.

        return null;
    }

    @Override
    public List<DiscountOnFullPaymentRecord> getListDiscountOnFullPaymentByConfigurationId(String configurationId) {
        // This is a mock implementation.
        // This function should query the database to get the list of discounts (% discount, e.g: 30% off) based on the configuration ID

        return null;
    }
}
