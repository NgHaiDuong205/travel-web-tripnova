package com.duong.travelweb.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Phản hồi thống kê cho một trang admin. */
public class AdminStatsDTO {
    private String section;
    private LocalDate from;
    private LocalDate to;
    private String granularity;
    private String currencyCode;
    private Map<String, BigDecimal> kpis = new LinkedHashMap<>();
    private List<StatPointDTO> series = new ArrayList<>();
    private Map<String, List<StatSliceDTO>> breakdowns = new LinkedHashMap<>();

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public LocalDate getFrom() {
        return from;
    }

    public void setFrom(LocalDate from) {
        this.from = from;
    }

    public LocalDate getTo() {
        return to;
    }

    public void setTo(LocalDate to) {
        this.to = to;
    }

    public String getGranularity() {
        return granularity;
    }

    public void setGranularity(String granularity) {
        this.granularity = granularity;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Map<String, BigDecimal> getKpis() {
        return kpis;
    }

    public void setKpis(Map<String, BigDecimal> kpis) {
        this.kpis = kpis;
    }

    public List<StatPointDTO> getSeries() {
        return series;
    }

    public void setSeries(List<StatPointDTO> series) {
        this.series = series;
    }

    public Map<String, List<StatSliceDTO>> getBreakdowns() {
        return breakdowns;
    }

    public void setBreakdowns(Map<String, List<StatSliceDTO>> breakdowns) {
        this.breakdowns = breakdowns;
    }
}
