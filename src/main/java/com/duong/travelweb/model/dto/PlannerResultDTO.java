package com.duong.travelweb.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Kết quả AI Planner (bản nháp, chưa lưu). warnings là mã: few_places, no_coordinates, no_hotel, no_restaurants, ai_unavailable, ai_quota. */
public class PlannerResultDTO {
    private String title;
    private String summary;
    private String hotelNote;
    private List<String> tips = new ArrayList<>();
    private boolean aiUsed;
    private String model;
    private List<String> warnings = new ArrayList<>();
    private UUID destinationId;
    private String destinationName;
    private String countryName;
    private Double centerLatitude;
    private Double centerLongitude;
    private LocalDate startDate;
    private LocalDate endDate;
    private int days;
    private int partySize;
    private String budget;
    private String pace;
    private List<String> interests = new ArrayList<>();
    private PlannerStopDTO hotel;
    private List<PlannerStopDTO> hotelAlternatives = new ArrayList<>();
    private List<PlannerDayDTO> dayPlans = new ArrayList<>();
    private BigDecimal estimatedTotal;
    private String prompt;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getHotelNote() {
        return hotelNote;
    }

    public void setHotelNote(String hotelNote) {
        this.hotelNote = hotelNote;
    }

    public List<String> getTips() {
        return tips;
    }

    public void setTips(List<String> tips) {
        this.tips = tips;
    }

    public boolean isAiUsed() {
        return aiUsed;
    }

    public void setAiUsed(boolean aiUsed) {
        this.aiUsed = aiUsed;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public UUID getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(UUID destinationId) {
        this.destinationId = destinationId;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public Double getCenterLatitude() {
        return centerLatitude;
    }

    public void setCenterLatitude(Double centerLatitude) {
        this.centerLatitude = centerLatitude;
    }

    public Double getCenterLongitude() {
        return centerLongitude;
    }

    public void setCenterLongitude(Double centerLongitude) {
        this.centerLongitude = centerLongitude;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }

    public int getPartySize() {
        return partySize;
    }

    public void setPartySize(int partySize) {
        this.partySize = partySize;
    }

    public String getBudget() {
        return budget;
    }

    public void setBudget(String budget) {
        this.budget = budget;
    }

    public String getPace() {
        return pace;
    }

    public void setPace(String pace) {
        this.pace = pace;
    }

    public List<String> getInterests() {
        return interests;
    }

    public void setInterests(List<String> interests) {
        this.interests = interests;
    }

    public PlannerStopDTO getHotel() {
        return hotel;
    }

    public void setHotel(PlannerStopDTO hotel) {
        this.hotel = hotel;
    }

    public List<PlannerStopDTO> getHotelAlternatives() {
        return hotelAlternatives;
    }

    public void setHotelAlternatives(List<PlannerStopDTO> hotelAlternatives) {
        this.hotelAlternatives = hotelAlternatives;
    }

    public List<PlannerDayDTO> getDayPlans() {
        return dayPlans;
    }

    public void setDayPlans(List<PlannerDayDTO> dayPlans) {
        this.dayPlans = dayPlans;
    }

    public BigDecimal getEstimatedTotal() {
        return estimatedTotal;
    }

    public void setEstimatedTotal(BigDecimal estimatedTotal) {
        this.estimatedTotal = estimatedTotal;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
