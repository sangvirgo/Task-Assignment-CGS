package com.demo.packing.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public final class DateTimeUtil {
    private static final int PERIOD_MONTH = 1;
    private static final int PERIOD_YEAR = 2;

    private DateTimeUtil() {
    }

    public static Long getLocalDateTime(LocalDateTime dateTime) {
        ZonedDateTime zdt = ZonedDateTime.of(dateTime, ZoneId.systemDefault());
        return zdt.toInstant().toEpochMilli();
    }

    public static LocalDateTime toLocalDateTime(Long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDateTime();
    }

    public static LocalDateTime getToDate(Long fromDate, Integer period, Integer periodValue) {
        LocalDateTime from = toLocalDateTime(fromDate);
        if (period == PERIOD_MONTH) {
            return from.plusMonths(periodValue).plusHours(23).plusMinutes(59).plusSeconds(59);
        }
        if (period == PERIOD_YEAR) {
            return from.plusYears(periodValue).plusHours(23).plusMinutes(59).plusSeconds(59);
        }
        throw new IllegalArgumentException("Unexpected period value: " + period);
    }
}
