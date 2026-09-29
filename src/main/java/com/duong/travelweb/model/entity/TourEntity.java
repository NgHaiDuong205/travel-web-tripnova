package com.duong.travelweb.model.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Tour trọn gói (lịch trình nhiều ngày). */
@Entity
@Table(name = "tours")
public class TourEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id")
    private DestinationEntity destination;

    @Column(name = "name")
    private String name;

    @Column(name = "slug")
    private String slug;

    @Column(name = "description")
    private String description;

    @Column(name = "itinerary")
    private String itinerary;

    @Column(name = "duration_days")
    private Short durationDays;

    @Column(name = "duration_nights")
    private Short durationNights;

    @Column(name = "max_participants")
    private Short maxParticipants;

    @Column(name = "min_participants")
    private Short minParticipants;

    @Column(name = "price_adult")
    private BigDecimal priceAdult;

    @Column(name = "price_child")
    private BigDecimal priceChild;

    @Column(name = "departure_date")
    private LocalDate departureDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @Column(name = "departure_location")
    private String departureLocation;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "highlights")
    private String highlights;

    @Column(name = "included")
    private String included;

    @Column(name = "excluded")
    private String excluded;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DestinationEntity getDestination() {
        return destination;
    }

    public void setDestination(DestinationEntity destination) {
        this.destination = destination;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getItinerary() {
        return itinerary;
    }

    public void setItinerary(String itinerary) {
        this.itinerary = itinerary;
    }

    public Short getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(Short durationDays) {
        this.durationDays = durationDays;
    }

    public Short getDurationNights() {
        return durationNights;
    }

    public void setDurationNights(Short durationNights) {
        this.durationNights = durationNights;
    }

    public Short getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Short maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public Short getMinParticipants() {
        return minParticipants;
    }

    public void setMinParticipants(Short minParticipants) {
        this.minParticipants = minParticipants;
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

    public String getHighlights() {
        return highlights;
    }

    public void setHighlights(String highlights) {
        this.highlights = highlights;
    }

    public String getIncluded() {
        return included;
    }

    public void setIncluded(String included) {
        this.included = included;
    }

    public String getExcluded() {
        return excluded;
    }

    public void setExcluded(String excluded) {
        this.excluded = excluded;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

