package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.RoomTypeImageDTO;
import com.duong.travelweb.model.dto.RoomTypeImageRequestDTO;

import java.util.List;
import java.util.UUID;

public interface RoomTypeImageService {
    /**
     * Ảnh của hạng phòng theo sortOrder. 404 nếu hạng phòng không thuộc khách sạn;
     * activeOnly (public) còn 404 khi khách sạn / hạng phòng đã ẩn.
     */
    List<RoomTypeImageDTO> findByRoomType(UUID hotelId, UUID roomTypeId, boolean activeOnly);

    RoomTypeImageDTO add(UUID userId, UUID hotelId, UUID roomTypeId, RoomTypeImageRequestDTO request);

    RoomTypeImageDTO update(UUID hotelId, UUID roomTypeId, UUID imageId, RoomTypeImageRequestDTO request);

    void delete(UUID hotelId, UUID roomTypeId, UUID imageId);

    /** Đặt ảnh làm room_types.cover_image_url. */
    RoomTypeImageDTO setCover(UUID hotelId, UUID roomTypeId, UUID imageId);
}
