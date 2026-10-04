package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.HotelEntity;
import com.duong.travelweb.repository.custom.HotelRepositoryCustom;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
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

    /** Khách sạn do một manager quản lý (kể cả đã ẩn), theo tên. */
    @Query("SELECT h FROM HotelEntity h WHERE h.managedBy.id = :userId ORDER BY h.name")
    List<HotelEntity> findByManagerId(@Param("userId") UUID userId);

    @Query("SELECT h FROM HotelEntity h LEFT JOIN FETCH h.destination WHERE h.managedBy.id IN :userIds ORDER BY h.name")
    List<HotelEntity> findByManagerIds(@Param("userIds") List<UUID> userIds);

    @Query("SELECT COUNT(h) > 0 FROM HotelEntity h WHERE h.id = :hotelId AND h.managedBy.id = :userId")
    boolean isManagedBy(@Param("hotelId") UUID hotelId, @Param("userId") UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE hotels SET managed_by = NULL, updated_at = now() WHERE managed_by = :userId", nativeQuery = true)
    int clearManager(@Param("userId") UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE hotels SET managed_by = :userId, updated_at = now() WHERE id IN (:hotelIds)", nativeQuery = true)
    int assignManager(@Param("userId") UUID userId, @Param("hotelIds") List<UUID> hotelIds);

    /**
     * Khách sạn đang hoạt động gần một toạ độ: [id, km], gần nhất trước. Lọc thô bằng khung lat/lng
     * (dùng được index nếu có) rồi tính haversine; service tự loại kết quả ngoài bán kính.
     */
    @Query(value = """
            SELECT h.id, 6371 * 2 * ASIN(SQRT(
                       POWER(SIN(RADIANS(h.latitude - :lat) / 2), 2)
                     + COS(RADIANS(:lat)) * COS(RADIANS(h.latitude)) * POWER(SIN(RADIANS(h.longitude - :lng) / 2), 2))) AS km
            FROM hotels h
            WHERE h.is_active = true
              AND h.latitude BETWEEN :lat - :dLat AND :lat + :dLat
              AND h.longitude BETWEEN :lng - :dLng AND :lng + :dLng
            ORDER BY km
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findNearby(@Param("lat") double lat, @Param("lng") double lng,
                              @Param("dLat") double dLat, @Param("dLng") double dLng, @Param("limit") int limit);

    /** Khoá dòng khách sạn (VD khi thêm ảnh) để các request đồng thời xếp hàng. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM HotelEntity h WHERE h.id = :id")
    Optional<HotelEntity> lockById(@Param("id") UUID id);

    /**
     * Khách sạn cho AI Planner: id, name, star_rating, cover, address, lat, lng, giá/đêm rẻ nhất của hạng phòng
     * đủ :occupancy người (bỏ khách sạn không có hạng phòng phù hợp), avg_rating, review_count, same_point.
     */
    @Query(value = """
            SELECT h.id, h.name, h.star_rating, h.cover_image_url, h.address, h.latitude, h.longitude, p.min_price,
                   r.avg_rating, COALESCE(r.review_count, 0),
                   CASE WHEN h.latitude IS NULL THEN 0 ELSE count(*) OVER (PARTITION BY h.latitude, h.longitude) END
            FROM hotels h
            JOIN LATERAL (
                SELECT min(rt.price_per_night) AS min_price
                FROM room_types rt
                WHERE rt.hotel_id = h.id AND rt.is_active = true AND COALESCE(rt.max_occupancy, 2) >= :occupancy
            ) p ON p.min_price IS NOT NULL
            LEFT JOIN LATERAL (
                SELECT avg(po.rating) AS avg_rating, count(*) AS review_count
                FROM posts po
                WHERE po.entity_type = 'hotel' AND po.entity_id = h.id AND po.status = 'approved' AND po.rating IS NOT NULL
            ) r ON true
            WHERE h.destination_id = :destinationId AND h.is_active = true
            """, nativeQuery = true)
    List<Object[]> findPlannerCandidates(@Param("destinationId") UUID destinationId, @Param("occupancy") int occupancy);
}
