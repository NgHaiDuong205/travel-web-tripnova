package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.ItineraryItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItineraryItemRepository extends JpaRepository<ItineraryItemEntity, UUID> {

    /** Sắp theo ngày, thứ tự, giờ bắt đầu (giờ trống xếp cuối). */
    @Query("SELECT it FROM ItineraryItemEntity it WHERE it.itinerary.id = :itineraryId " +
           "ORDER BY it.dayNumber, it.sortOrder, it.startTime NULLS LAST, it.createdAt")
    List<ItineraryItemEntity> findByItineraryId(@Param("itineraryId") UUID itineraryId);

    @Query("SELECT it FROM ItineraryItemEntity it WHERE it.id = :itemId AND it.itinerary.id = :itineraryId")
    Optional<ItineraryItemEntity> findInItinerary(@Param("itemId") UUID itemId, @Param("itineraryId") UUID itineraryId);

    @Query("SELECT COALESCE(MAX(it.sortOrder), -1) FROM ItineraryItemEntity it " +
           "WHERE it.itinerary.id = :itineraryId AND it.dayNumber = :dayNumber")
    int findMaxSortOrder(@Param("itineraryId") UUID itineraryId, @Param("dayNumber") short dayNumber);

    @Query("SELECT COALESCE(MAX(it.dayNumber), 0) FROM ItineraryItemEntity it WHERE it.itinerary.id = :itineraryId")
    int findMaxDayNumber(@Param("itineraryId") UUID itineraryId);
}
