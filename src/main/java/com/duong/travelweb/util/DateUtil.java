package com.duong.travelweb.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class DateUtil {
    public static LocalDate parseLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        String strValue = value.toString().trim();
        if (strValue.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(strValue);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
