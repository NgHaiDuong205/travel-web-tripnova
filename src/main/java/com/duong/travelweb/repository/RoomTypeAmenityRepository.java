package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.RoomTypeAmenityEntity;
import com.duong.travelweb.model.entity.RoomTypeAmenityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RoomTypeAmenityRepository extends JpaRepository<RoomTypeAmenityEntity, RoomTypeAmenityId> {

    /** [roomTypeId, amenityId, amenityName] */
    @Query("SELECT rta.roomType.id, rta.amenity.id, rta.amenity.name FROM RoomTypeAmenityEntity rta " +
           "WHERE rta.roomType.id IN :roomTypeIds ORDER BY rta.amenity.name")
    List<Object[]> findAmenitiesByRoomTypeIds(@Param("roomTypeIds") List<UUID> roomTypeIds);

    /** Chèn thẳng bằng SQL: tránh merge() của entity @IdClass (Hibernate phải SELECT kèm quan hệ trước). */
    @Modifying
    @Query(value = "INSERT INTO room_type_amenities (room_type_id, amenity_id) VALUES (:roomTypeId, :amenityId) " +
                   "ON CONFLICT DO NOTHING", nativeQuery = true)
    int insertLink(@Param("roomTypeId") UUID roomTypeId, @Param("amenityId") UUID amenityId);

    @Modifying
    @Query("DELETE FROM RoomTypeAmenityEntity rta WHERE rta.roomType.id = :roomTypeId")
    int deleteByRoomTypeId(@Param("roomTypeId") UUID roomTypeId);
}
