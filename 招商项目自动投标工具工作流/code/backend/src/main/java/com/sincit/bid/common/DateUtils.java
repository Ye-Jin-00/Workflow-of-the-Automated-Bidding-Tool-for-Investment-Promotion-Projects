package com.sincit.bid.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/**
 * 日期解析工具，兼容前端日期组件常见的几种格式。
 */
public final class DateUtils {

    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    );

    private DateUtils() {
    }

    public static LocalDateTime parseDateTime(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String value = text.trim();
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        try {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atTime(18, 0);
        } catch (DateTimeParseException e) {
            throw BusinessException.invalid("时间格式不正确: " + text + "，请使用 yyyy-MM-dd HH:mm:ss");
        }
    }
}
