package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.HotelEntity;
import com.duong.travelweb.repository.custom.HotelRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface HotelRepository extends JpaRepository<HotelEntity, UUID>, JpaSpecificationExecutor<HotelEntity>, HotelRepositoryCustom {
    
    @Query("SELECT h FROM HotelEntity h WHERE h.destination.id = :destinationId")
    List<HotelEntity> findByDestinationId(@Param("destinationId") UUID destinationId);
    
    @Query("SELECT h FROM HotelEntity h WHERE h.name LIKE %:name%")
    List<HotelEntity> findByNameContaining(@Param("name") String name);
    
    @Query("SELECT h FROM HotelEntity h WHERE h.starRating >= :minRating")
    List<HotelEntity> findByStarRatingGreaterThanEqual(@Param("minRating") Integer minRating);
}
