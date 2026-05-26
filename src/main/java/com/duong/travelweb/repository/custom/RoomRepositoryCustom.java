package com.duong.travelweb.repository.custom;

import com.duong.travelweb.builder.RoomSearchBuilder;
import com.duong.travelweb.model.entity.RoomEntity;

import java.util.List;
import java.util.UUID;

public interface RoomRepositoryCustom {
    List<RoomEntity> searchRooms(UUID hotelId, RoomSearchBuilder searchBuilder);
}
