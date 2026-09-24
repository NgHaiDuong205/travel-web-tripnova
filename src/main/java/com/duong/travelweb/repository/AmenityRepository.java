package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.AmenityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AmenityRepository extends JpaRepository<AmenityEntity, UUID> {

    @Query("SELECT a FROM AmenityEntity a ORDER BY a.category, a.name")
    List<AmenityEntity> findAllOrdered();

    @Query("SELECT a FROM AmenityEntity a WHERE a.category = :category ORDER BY a.name")
    List<AmenityEntity> findByCategory(@Param("category") String category);

    /** Trùng tên trong cùng nhóm (không phân biệt hoa thường); excludeId = null khi tạo mới. */
    @Query("SELECT COUNT(a) > 0 FROM AmenityEntity a WHERE LOWER(a.name) = LOWER(:name) " +
           "AND a.category = :category AND (:excludeId IS NULL OR a.id <> :excludeId)")
    boolean existsByNameInCategory(@Param("name") String name,
                                   @Param("category") String category,
                                   @Param("excludeId") UUID excludeId);

    /** [amenityId, số khách sạn dùng] */
    @Query(value = "SELECT amenity_id, COUNT(*) FROM hotel_amenities GROUP BY amenity_id", nativeQuery = true)
    List<Object[]> countHotelsByAmenity();

    /** [amenityId, số hạng phòng dùng] */
    @Query(value = "SELECT amenity_id, COUNT(*) FROM room_type_amenities GROUP BY amenity_id", nativeQuery = true)
    List<Object[]> countRoomTypesByAmenity();
}
