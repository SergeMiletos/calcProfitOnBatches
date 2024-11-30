package com.agzr.calcProfitOnBatches.formatters;

import org.openxava.formatters.IFormatter;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class LocalDateTimeFormatter implements IFormatter {

    public String format(HttpServletRequest request, Object localDateTime) {
        if (localDateTime == null) {
            return "";
        }
        return localDateTime.toString();
    }

    public Object parse(HttpServletRequest request, String localDateTime) {
        return localDateTime.isEmpty() ? "" : LocalDateTime.parse(localDateTime);
    }
}
