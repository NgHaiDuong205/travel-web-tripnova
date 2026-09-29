package com.duong.travelweb.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Body tạo / sửa tour (admin). */
public class TourRequestDTO {
    @NotBlank(message = "Tên tour không được để trống")
    @Size(max = 255, message = "Tên tour tối đa 255 ký tự")
    private String name;

    private UUID destinationId;

    private String description;

    @NotNull(message = "Thiếu số ngày")
    @Min(value = 1, message = "Số ngày từ 1 đến 60")
    @Max(value = 60, message = "Số ngày từ 1 đến 60")
    private Integer durationDays;

    @Min(value = 0, message = "Số đêm không hợp lệ")
    @Max(value = 60, message = "Số đêm tối đa 60")
    private Integer durationNights;

    @Min(value = 1, message = "Số khách tối thiểu từ 1")
    @Max(value = 500, message = "Số khách tối thiểu tối đa 500")
    private Integer minParticipants;

    @Min(value = 1, message = "Số chỗ tối đa phải từ 1")
    @Max(value = 500, message = "Số chỗ tối đa tối đa 500")
    private Integer maxParticipants;

    @NotNull(message = "Thiếu giá người lớn")
    @DecimalMin(value = "0.01", message = "Giá phải lớn hơn 0")
    @Digits(integer = 10, fraction = 2, message = "Giá không hợp lệ")
    private BigDecimal priceAdult;

    @DecimalMin(value = "0", message = "Giá trẻ em không hợp lệ")
    @Digits(integer = 10, fraction = 2, message = "Giá không hợp lệ")
    private BigDecimal priceChild;

    private LocalDate departureDate;

    private LocalDate returnDate;

    @Size(max = 255, message = "Điểm khởi hành tối đa 255 ký tự")
    private String departureLocation;

    @Size(max = 1000, message = "URL ảnh tối đa 1000 ký tự")
    private String coverImageUrl;

    @Size(max = 12, message = "Tối đa 12 điểm nổi bật")
    private List<@NotBlank(message = "Điểm nổi bật không được trống") @Size(max = 120, message = "Điểm nổi bật tối đa 120 ký tự") String> highlights;

    @Size(max = 30, message = "Tối đa 30 mục bao gồm")
    private List<@NotBlank(message = "Mục bao gồm không được trống") String> included;

    @Size(max = 30, message = "Tối đa 30 mục không bao gồm")
    private List<@NotBlank(message = "Mục không bao gồm không được trống") String> excluded;

    @Size(max = 30, message = "Tối đa 30 chặng lịch trình")
    private List<@Valid TourDayDTO> itinerary;

    private Boolean isActive;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(UUID destinationId) {
        this.destinationId = destinationId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }

    public Integer getDurationNights() {
        return durationNights;
    }

    public void setDurationNights(Integer durationNights) {
        this.durationNights = durationNights;
    }

    public Integer getMinParticipants() {
        return minParticipants;
    }

    public void setMinParticipants(Integer minParticipants) {
        this.minParticipants = minParticipants;
    }

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public BigDecimal getPriceAdult() {
        return priceAdult;
    }

    public void setPriceAdult(BigDecimal priceAdult) {
        this.priceAdult = priceAdult;
    }

    public BigDecimal getPriceChild() {
        return priceChild;
    }

    public void setPriceChild(BigDecimal priceChild) {
        this.priceChild = priceChild;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(LocalDate departureDate) {
        this.departureDate = departureDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public String getDepartureLocation() {
        return departureLocation;
    }

    public void setDepartureLocation(String departureLocation) {
        this.departureLocation = departureLocation;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public List<String> getHighlights() {
        return highlights;
    }

    public void setHighlights(List<String> highlights) {
        this.highlights = highlights;
    }

    public List<String> getIncluded() {
        return included;
    }

    public void setIncluded(List<String> included) {
        this.included = included;
    }

    public List<String> getExcluded() {
        return excluded;
    }

    public void setExcluded(List<String> excluded) {
        this.excluded = excluded;
    }

    public List<TourDayDTO> getItinerary() {
        return itinerary;
    }

    public void setItinerary(List<TourDayDTO> itinerary) {
        this.itinerary = itinerary;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}

