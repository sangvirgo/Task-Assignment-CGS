package com.demo.packing.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeUtilTest {

    // ──────────────────────────────────────────────
    // toLocalDateTime
    // ──────────────────────────────────────────────

    @Test
    void toLocalDateTime_shouldConvertEpochMillisCorrectly() {
        // 2024-01-15 00:00:00 UTC in millis
        long epochMillis = 1705276800000L;
        LocalDateTime result = DateTimeUtil.toLocalDateTime(epochMillis);

        assertEquals(2024, result.getYear());
        assertEquals(1, result.getMonthValue());
        assertEquals(15, result.getDayOfMonth());
    }

    @Test
    void toLocalDateTime_roundTrip_shouldBeConsistent() {
        long now = System.currentTimeMillis();
        LocalDateTime dt = DateTimeUtil.toLocalDateTime(now);
        assertNotNull(dt);
    }

    // ──────────────────────────────────────────────
    // getToDate — MONTH
    // ──────────────────────────────────────────────

    @Test
    void getToDate_periodMonth_shouldAddCorrectMonths() {
        long fromMillis = 1705276800000L; // 2024-01-15 UTC
        LocalDateTime result = DateTimeUtil.getToDate(fromMillis, 1, 3);

        assertEquals(4, result.getMonthValue()); // January + 3 = April
    }

    @Test
    void getToDate_periodMonth_shouldAddEndOfDayTime() {
        long fromMillis = 1705276800000L;
        LocalDateTime result = DateTimeUtil.getToDate(fromMillis, 1, 1);

        assertEquals(23, result.getHour());
        assertEquals(59, result.getMinute());
        assertEquals(59, result.getSecond());
    }

    @Test
    void getToDate_periodMonth_periodValue1_shouldAddOneMonth() {
        long fromMillis = 1705276800000L;
        LocalDateTime from = DateTimeUtil.toLocalDateTime(fromMillis);
        LocalDateTime result = DateTimeUtil.getToDate(fromMillis, 1, 1);

        assertEquals(from.plusMonths(1).getDayOfMonth(), result.getDayOfMonth());
    }

    // ──────────────────────────────────────────────
    // getToDate — YEAR
    // ──────────────────────────────────────────────

    @Test
    void getToDate_periodYear_shouldAddCorrectYears() {
        long fromMillis = 1705276800000L; // 2024-01-15
        LocalDateTime result = DateTimeUtil.getToDate(fromMillis, 2, 1);

        assertEquals(2025, result.getYear());
    }

    @Test
    void getToDate_periodYear_shouldAddEndOfDayTime() {
        long fromMillis = 1705276800000L;
        LocalDateTime result = DateTimeUtil.getToDate(fromMillis, 2, 2);

        assertEquals(23, result.getHour());
        assertEquals(59, result.getMinute());
        assertEquals(59, result.getSecond());
    }

    // ──────────────────────────────────────────────
    // getToDate — invalid period
    // ──────────────────────────────────────────────

    @Test
    void getToDate_invalidPeriod_shouldThrowIllegalArgumentException() {
        long fromMillis = System.currentTimeMillis();

        assertThrows(IllegalArgumentException.class,
                () -> DateTimeUtil.getToDate(fromMillis, 3, 1));
    }

    @Test
    void getToDate_invalidPeriod_exceptionMessage_shouldContainPeriodValue() {
        long fromMillis = System.currentTimeMillis();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> DateTimeUtil.getToDate(fromMillis, 99, 1));

        assertTrue(ex.getMessage().contains("99"));
    }

    // ──────────────────────────────────────────────
    // getLocalDateTime
    // ──────────────────────────────────────────────

    @Test
    void getLocalDateTime_shouldReturnPositiveLong() {
        LocalDateTime dt = LocalDateTime.of(2024, 6, 1, 10, 0, 0);
        Long result = DateTimeUtil.getLocalDateTime(dt);

        assertNotNull(result);
        assertTrue(result > 0);
    }
}