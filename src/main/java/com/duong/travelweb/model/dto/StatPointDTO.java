package com.duong.travelweb.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** Một điểm dữ liệu theo thời gian. */
public class StatPointDTO {
    private LocalDate period;
    private Map<String, BigDecimal> values = new LinkedHashMap<>();

    public StatPointDTO() {
    }

    public StatPointDTO(LocalDate period) {
        this.period = period;
    }

    public LocalDate getPeriod() {
        return period;
    }

    public void setPeriod(LocalDate period) {
        this.period = period;
    }

    public Map<String, BigDecimal> getValues() {
        return values;
    }

    public void setValues(Map<String, BigDecimal> values) {
        this.values = values;
    }
}
