package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.RoomTypeImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RoomTypeImageRepository extends JpaRepository<RoomTypeImageEntity, UUID> {

    @Query("SELECT i FROM RoomTypeImageEntity i WHERE i.roomTypeId = :roomTypeId ORDER BY i.sortOrder, i.createdAt, i.id")
    List<RoomTypeImageEntity> findByRoomTypeId(@Param("roomTypeId") UUID roomTypeId);

    @Query("SELECT COUNT(i) FROM RoomTypeImageEntity i WHERE i.roomTypeId = :roomTypeId")
    long countByRoomTypeId(@Param("roomTypeId") UUID roomTypeId);

    @Query("SELECT COALESCE(MAX(i.sortOrder), -1) FROM RoomTypeImageEntity i WHERE i.roomTypeId = :roomTypeId")
    int maxSortOrder(@Param("roomTypeId") UUID roomTypeId);
}
