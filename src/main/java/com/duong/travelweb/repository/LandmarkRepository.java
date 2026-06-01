package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.LandmarkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LandmarkRepository extends JpaRepository<LandmarkEntity, UUID> {
    @Query("SELECT l FROM LandmarkEntity l WHERE l.destination.id = :destinationId AND l.isActive = true AND " +
           "(:category IS NULL OR :category = '' OR LOWER(CAST(l.category AS string)) = LOWER(:category)) " +
           "ORDER BY l.name ASC")
    List<LandmarkEntity> findActiveLandmarksByDestinationId(@Param("destinationId") UUID destinationId, 
                                                            @Param("category") String category);

    @Query("SELECT l FROM LandmarkEntity l WHERE l.isActive = true AND " +
           "(:category IS NULL OR :category = '' OR LOWER(CAST(l.category AS string)) = LOWER(:category)) " +
           "ORDER BY l.name ASC")
    List<LandmarkEntity> findAllActiveLandmarks(@Param("category") String category);

    @Query("SELECT l FROM LandmarkEntity l WHERE l.id = :landmarkId AND l.destination.id = :destinationId AND l.isActive = true AND l.destination.isActive = true")
    Optional<LandmarkEntity> findActiveLandmarkByIdAndDestinationId(@Param("landmarkId") UUID landmarkId, 
                                                                    @Param("destinationId") UUID destinationId);
}
