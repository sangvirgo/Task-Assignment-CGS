package com.demo.packing.service;

import com.demo.packing.dto.ItemDetailsRes;
import com.demo.packing.dto.PreBookingPackageReq;
import com.demo.packing.dto.PriceSummaryRes;
import com.demo.packing.record.PackageRecord;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PackagePricingServiceImplTest {

    private PackagePricingServiceImpl service;
    private PackageRecord samplePackage;

    private static final BigDecimal OFFICIAL_PRICE = BigDecimal.valueOf(80.00);
    private static final int PERIOD_MONTH = 1;
    private static final int PERIOD_YEAR = 2;

    @BeforeEach
    void setUp() {
        service = new PackagePricingServiceImpl();
        samplePackage = new PackageRecord(
                "pkg-001", "config123", "Standard Package",
                1, 10, "US", "NYC", "Manhattan", "A", "Venue A",
                BigDecimal.valueOf(100.00), OFFICIAL_PRICE, 5,
                "Description", "Terms", "image_url",
                1, 1, null,
                LocalDateTime.now(), "admin",
                LocalDateTime.now(), "admin"
        );
    }

    // ──────────────────────────────────────────────
    // getPackageById
    // ──────────────────────────────────────────────

    @Test
    void getPackageById_shouldReturnPackageWithCorrectId() {
        PackageRecord result = service.getPackageById("pkg-test");
        assertEquals("pkg-test", result.getPackageId());
        assertNotNull(result.getOfficialPrice());
    }

    // ──────────────────────────────────────────────
    // checkAvailablePackage
    // ──────────────────────────────────────────────

    @Test
    void checkAvailablePackage_shouldReturnTrue() {
        assertTrue(service.checkAvailablePackage(
                LocalDateTime.now(), LocalDateTime.now().plusMonths(3), samplePackage));
    }

    // ──────────────────────────────────────────────
    // getItemDetailsById
    // ──────────────────────────────────────────────

    @Test
    void getItemDetailsById_shouldReturnItemWithCorrectId() {
        ItemDetailsRes item = service.getItemDetailsById("item-001", "company-abc");
        assertEquals("item-001", item.getId());
        assertNotNull(item.getPrice());
        assertEquals(1, item.getStatus());
    }

    // ──────────────────────────────────────────────
    // calculateBookingPackagePrice — no items
    // ──────────────────────────────────────────────

    @Test
    void calculatePrice_noItems_packageTotalPrice_shouldEqual_officialPrice_times_periodValue() {
        PreBookingPackageReq req = buildReq(PERIOD_MONTH, 3);

        PriceSummaryRes res = service.calculateBookingPackagePrice(req, samplePackage, null, List.of());

        assertEquals(0, BigDecimal.valueOf(240.00).compareTo(res.getPackageTotalPrice()));
    }

    @Test
    void calculatePrice_noItems_packagePrice_shouldBe_officialPrice() {
        PreBookingPackageReq req = buildReq(PERIOD_MONTH, 3);

        PriceSummaryRes res = service.calculateBookingPackagePrice(req, samplePackage, null, List.of());

        assertEquals(0, OFFICIAL_PRICE.compareTo(res.getPackagePrice()));
    }

    @Test
    void calculatePrice_noItems_subTotal_shouldEqual_packageTotalPrice() {
        PreBookingPackageReq req = buildReq(PERIOD_MONTH, 3);

        PriceSummaryRes res = service.calculateBookingPackagePrice(req, samplePackage, null, List.of());

        assertEquals(0, res.getPackageTotalPrice().compareTo(res.getSubTotal()));
    }

    @Test
    void calculatePrice_noItems_taxShouldBeZero() {
        PreBookingPackageReq req = buildReq(PERIOD_MONTH, 3);

        PriceSummaryRes res = service.calculateBookingPackagePrice(req, samplePackage, null, List.of());

        assertEquals(0.0, res.getTaxPercent());
    }

    @Test
    void calculatePrice_noItems_packageIdAndNameShouldMatch() {
        PreBookingPackageReq req = buildReq(PERIOD_MONTH, 3);

        PriceSummaryRes res = service.calculateBookingPackagePrice(req, samplePackage, null, List.of());

        assertEquals("pkg-001", res.getPackageId());
        assertEquals("Standard Package", res.getPackageName());
    }

    @Test
    void calculatePrice_noItems_toDate_shouldBeAfterFromDate() {
        PreBookingPackageReq req = buildReq(PERIOD_MONTH, 3);

        PriceSummaryRes res = service.calculateBookingPackagePrice(req, samplePackage, null, List.of());

        assertNotNull(res.getToDate());
        assertTrue(res.getToDate() > req.getFromDate());
    }

    @Test
    void calculatePrice_noItems_discountFieldsShouldBePresent() {
        PreBookingPackageReq req = buildReq(PERIOD_MONTH, 3);

        PriceSummaryRes res = service.calculateBookingPackagePrice(req, samplePackage, null, List.of());

        assertNotNull(res.getFullPaymentOptionDiscount());
        assertNotNull(res.getPayMonthlyOptionDiscount());
        assertNotNull(res.getPackageFullPaymentPrice());
        assertNotNull(res.getPackagePayMonthlyPrice());
        assertNotNull(res.getPayMonthlyPrice());
    }

    // ──────────────────────────────────────────────
    // calculateBookingPackagePrice — with items
    // ──────────────────────────────────────────────

    @Test
    void calculatePrice_withItems_customizeItem_shouldNotBeNull() {
        PreBookingPackageReq req = buildReqWithItems("item-001", List.of("space-1", "space-2"));

        ItemDetailsRes item = new ItemDetailsRes()
                .setId("item-001")
                .setItemName("Sample Item")
                .setStatus(1)
                .setPrice(BigDecimal.valueOf(25.00));

        PriceSummaryRes res = service.calculateBookingPackagePrice(req, samplePackage, null, List.of(item));

        assertNotNull(res.getCustomizeItem());
        assertNotNull(res.getDefaultItem());
    }

    @Test
    void calculatePrice_withItems_subTotal_shouldIncludeCustomItemPrice() {
        // 2 spaces × 25.00 = 50; packageTotal = 80 × 3 = 240; subTotal = 290
        PreBookingPackageReq req = buildReqWithItems("item-001", List.of("space-1", "space-2"));

        ItemDetailsRes item = new ItemDetailsRes()
                .setId("item-001")
                .setItemName("Sample Item")
                .setStatus(1)
                .setPrice(BigDecimal.valueOf(25.00));

        PriceSummaryRes res = service.calculateBookingPackagePrice(req, samplePackage, null, List.of(item));

        assertEquals(0, BigDecimal.valueOf(290.00).compareTo(res.getSubTotal()));
    }

    @Test
    void calculatePrice_withZeroPriceItem_shouldGoToDefaultList() {
        PreBookingPackageReq req = buildReqWithItems("item-free", List.of("space-1"));

        ItemDetailsRes freeItem = new ItemDetailsRes()
                .setId("item-free")
                .setItemName("Free Item")
                .setStatus(1)
                .setPrice(BigDecimal.ZERO);

        PriceSummaryRes res = service.calculateBookingPackagePrice(req, samplePackage, null, List.of(freeItem));

        assertFalse(res.getDefaultItem().getItemInfoList().isEmpty());
        assertTrue(res.getCustomizeItem().getItemInfoList().isEmpty());
    }

    // ──────────────────────────────────────────────
    // calculateBookingPackagePrice — YEAR period
    // ──────────────────────────────────────────────

    @Test
    void calculatePrice_yearPeriod_packageTotalPrice_shouldEqual_officialPrice_times_periodValue() {
        PreBookingPackageReq req = buildReq(PERIOD_YEAR, 2);

        PriceSummaryRes res = service.calculateBookingPackagePrice(req, samplePackage, null, List.of());

        assertEquals(0, BigDecimal.valueOf(160.00).compareTo(res.getPackageTotalPrice()));
    }

    // ──────────────────────────────────────────────
    // helpers
    // ──────────────────────────────────────────────

    private PreBookingPackageReq buildReq(int period, int periodValue) {
        return PreBookingPackageReq.fromJson(new JsonObject()
                .put("fromDate", System.currentTimeMillis())
                .put("period", period)
                .put("periodValue", periodValue)
                .put("packageId", "pkg-001"));
    }

    private PreBookingPackageReq buildReqWithItems(String itemId, List<String> spaceIds) {
        JsonArray spaces = new JsonArray();
        spaceIds.forEach(spaces::add);

        JsonArray list = new JsonArray()
                .add(new JsonObject()
                        .put("itemDesignId", itemId)
                        .put("spaceIdList", spaces));

        return PreBookingPackageReq.fromJson(new JsonObject()
                .put("fromDate", System.currentTimeMillis())
                .put("period", PERIOD_MONTH)
                .put("periodValue", 3)
                .put("packageId", "pkg-001")
                .put("list", list));
    }
}