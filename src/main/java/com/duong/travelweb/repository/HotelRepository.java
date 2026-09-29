package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.HotelEntity;
import com.duong.travelweb.repository.custom.HotelRepositoryCustom;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HotelRepository extends JpaRepository<HotelEntity, UUID>, JpaSpecificationExecutor<HotelEntity>, HotelRepositoryCustom {
    
    @Query("SELECT h FROM HotelEntity h WHERE h.destination.id = :destinationId")
    List<HotelEntity> findByDestinationId(@Param("destinationId") UUID destinationId);
    
    @Query("SELECT h FROM HotelEntity h WHERE h.name LIKE %:name%")
    List<HotelEntity> findByNameContaining(@Param("name") String name);
    
    @Query("SELECT h FROM HotelEntity h WHERE h.starRating >= :minRating")
    List<HotelEntity> findByStarRatingGreaterThanEqual(@Param("minRating") Integer minRating);

    @Query("SELECT COUNT(h) FROM HotelEntity h WHERE h.isActive = true")
    long countActive();

    @Query("SELECT h.id, a.name FROM HotelEntity h JOIN h.hotelAmenities a WHERE h.id IN :hotelIds")
    List<Object[]> findAmenityNamesByHotelIds(@Param("hotelIds") List<UUID> hotelIds);

    /** Khoá dòng khách sạn (VD khi thêm ảnh) để các request đồng thời xếp hàng. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM HotelEntity h WHERE h.id = :id")
    Optional<HotelEntity> lockById(@Param("id") UUID id);
}
