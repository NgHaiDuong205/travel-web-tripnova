package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Body tạo/sửa khách sạn (admin). */
public class HotelRequestDTO {
    @NotNull(message = "Thiếu điểm đến")
    private UUID destinationId;

    @NotBlank(message = "Tên khách sạn không được trống")
    @Size(max = 200, message = "Tên tối đa 200 ký tự")
    private String name;

    private String description;

    @NotBlank(message = "Địa chỉ không được trống")
    private String address;

    @NotNull(message = "Thiếu hạng sao")
    @Min(value = 1, message = "Hạng sao từ 1 đến 5")
    @Max(value = 5, message = "Hạng sao từ 1 đến 5")
    private Integer starRating;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?$", message = "Giờ phải có dạng HH:mm")
    private String checkInTime;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?$", message = "Giờ phải có dạng HH:mm")
    private String checkOutTime;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;

    @Email(message = "Email không hợp lệ")
    @Size(max = 255)
    private String email;

    @Pattern(regexp = "^(free|partial|strict)$", message = "Chính sách huỷ phải là free, partial hoặc strict")
    private String cancellationPolicy;

    @Min(value = 0, message = "Số giờ huỷ không hợp lệ")
    @Max(value = 720, message = "Số giờ huỷ tối đa 720")
    private Integer cancellationHours;

    private Boolean breakfastIncluded;

    private Boolean petFriendly;

    private String coverImageUrl;

    @DecimalMin(value = "-90", message = "Vĩ độ không hợp lệ")
    @DecimalMax(value = "90", message = "Vĩ độ không hợp lệ")
    private BigDecimal latitude;

    @DecimalMin(value = "-180", message = "Kinh độ không hợp lệ")
    @DecimalMax(value = "180", message = "Kinh độ không hợp lệ")
    private BigDecimal longitude;

    private UUID managedById;

    private List<UUID> amenityIds;

    private Boolean isActive;

    public UUID getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(UUID destinationId) {
        this.destinationId = destinationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getStarRating() {
        return starRating;
    }

    public void setStarRating(Integer starRating) {
        this.starRating = starRating;
    }

    public String getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(String checkInTime) {
        this.checkInTime = checkInTime;
    }

    public String getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(String checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCancellationPolicy() {
        return cancellationPolicy;
    }

    public void setCancellationPolicy(String cancellationPolicy) {
        this.cancellationPolicy = cancellationPolicy;
    }

    public Integer getCancellationHours() {
        return cancellationHours;
    }

    public void setCancellationHours(Integer cancellationHours) {
        this.cancellationHours = cancellationHours;
    }

    public Boolean getBreakfastIncluded() {
        return breakfastIncluded;
    }

    public void setBreakfastIncluded(Boolean breakfastIncluded) {
        this.breakfastIncluded = breakfastIncluded;
    }

    public Boolean getPetFriendly() {
        return petFriendly;
    }

    public void setPetFriendly(Boolean petFriendly) {
        this.petFriendly = petFriendly;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public UUID getManagedById() {
        return managedById;
    }

    public void setManagedById(UUID managedById) {
        this.managedById = managedById;
    }

    public List<UUID> getAmenityIds() {
        return amenityIds;
    }

    public void setAmenityIds(List<UUID> amenityIds) {
        this.amenityIds = amenityIds;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
