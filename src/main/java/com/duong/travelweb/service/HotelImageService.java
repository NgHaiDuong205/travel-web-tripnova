package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.HotelImageDTO;
import com.duong.travelweb.model.dto.HotelImageRequestDTO;

import java.util.List;
import java.util.UUID;

public interface HotelImageService {
    /** Ảnh theo sortOrder tăng dần. 404 nếu khách sạn không tồn tại (public: cả khi đã ẩn). */
    List<HotelImageDTO> findByHotel(UUID hotelId, boolean activeOnly);

    HotelImageDTO add(UUID adminId, UUID hotelId, HotelImageRequestDTO request);

    HotelImageDTO update(UUID hotelId, UUID imageId, HotelImageRequestDTO request);

    void delete(UUID hotelId, UUID imageId);

    /** Đặt ảnh làm cover_image_url của khách sạn. */
    HotelImageDTO setCover(UUID hotelId, UUID imageId);
}
