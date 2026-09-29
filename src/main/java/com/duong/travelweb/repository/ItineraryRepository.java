package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.ItineraryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItineraryRepository extends JpaRepository<ItineraryEntity, UUID> {

    @Query(value = "SELECT i FROM ItineraryEntity i LEFT JOIN FETCH i.destination d LEFT JOIN FETCH d.country " +
                   "WHERE i.userId = :userId ORDER BY i.updatedAt DESC",
           countQuery = "SELECT COUNT(i) FROM ItineraryEntity i WHERE i.userId = :userId")
    Page<ItineraryEntity> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT i FROM ItineraryEntity i LEFT JOIN FETCH i.destination d LEFT JOIN FETCH d.country " +
           "WHERE i.id = :id AND i.userId = :userId")
    Optional<ItineraryEntity> findOwned(@Param("id") UUID id, @Param("userId") UUID userId);

    /** [itineraryId, số hoạt động, tổng chi phí dự kiến] */
    @Query("SELECT it.itinerary.id, COUNT(it), COALESCE(SUM(it.estimatedCost), 0) FROM ItineraryItemEntity it " +
           "WHERE it.itinerary.id IN :ids GROUP BY it.itinerary.id")
    List<Object[]> summarizeItems(@Param("ids") List<UUID> ids);
}
