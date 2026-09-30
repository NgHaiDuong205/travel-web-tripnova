package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.AdminHotelDTO;
import com.duong.travelweb.model.dto.AdminStatsDTO;
import com.duong.travelweb.model.dto.HotelImageDTO;
import com.duong.travelweb.model.dto.HotelImageRequestDTO;
import com.duong.travelweb.model.dto.HotelRequestDTO;
import com.duong.travelweb.model.dto.RoomAvailabilityDTO;
import com.duong.travelweb.model.dto.RoomAvailabilityRequestDTO;
import com.duong.travelweb.model.dto.RoomDTO;
import com.duong.travelweb.model.dto.RoomRequestDTO;
import com.duong.travelweb.model.dto.RoomTypeDTO;
import com.duong.travelweb.model.dto.RoomTypeRequestDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Trang quản lý của khách sạn (role HOTEL_MANAGER): mọi thao tác chỉ trên khách sạn có managed_by = managerId.
 * Khách sạn của người khác → 404 (không để lộ là khách sạn có tồn tại).
 * Manager không tạo khách sạn, không đổi người quản lý và không tự bật lại khách sạn đã bị tắt.
 */
public interface ManagedHotelService {
    List<AdminHotelDTO> findHotels(UUID managerId);

    AdminHotelDTO getHotel(UUID managerId, UUID hotelId);

    AdminHotelDTO updateHotel(UUID managerId, UUID hotelId, HotelRequestDTO request);

    void deactivateHotel(UUID managerId, UUID hotelId);

    AdminHotelDTO updateHotelAmenities(UUID managerId, UUID hotelId, List<UUID> amenityIds);

    List<RoomTypeDTO> findRoomTypes(UUID managerId, UUID hotelId);

    RoomTypeDTO createRoomType(UUID managerId, UUID hotelId, RoomTypeRequestDTO request);

    RoomTypeDTO updateRoomType(UUID managerId, UUID hotelId, UUID roomTypeId, RoomTypeRequestDTO request);

    void deactivateRoomType(UUID managerId, UUID hotelId, UUID roomTypeId);

    List<RoomDTO> findRooms(UUID managerId, UUID hotelId, UUID roomTypeId);

    RoomDTO getRoom(UUID managerId, UUID hotelId, UUID roomId);

    RoomDTO createRoom(UUID managerId, UUID hotelId, RoomRequestDTO request);

    RoomDTO updateRoom(UUID managerId, UUID hotelId, UUID roomId, RoomRequestDTO request);

    void deleteRoom(UUID managerId, UUID hotelId, UUID roomId);

    List<RoomAvailabilityDTO> getRoomAvailability(UUID managerId, UUID roomId, LocalDate from, LocalDate to);

    List<RoomAvailabilityDTO> updateRoomAvailability(UUID managerId, UUID roomId, RoomAvailabilityRequestDTO request);

    List<HotelImageDTO> findImages(UUID managerId, UUID hotelId);

    HotelImageDTO addImage(UUID managerId, UUID hotelId, HotelImageRequestDTO request);

    HotelImageDTO updateImage(UUID managerId, UUID hotelId, UUID imageId, HotelImageRequestDTO request);

    HotelImageDTO setCoverImage(UUID managerId, UUID hotelId, UUID imageId);

    void deleteImage(UUID managerId, UUID hotelId, UUID imageId);

    /** Thống kê khách sạn của manager; hotelId = null → gộp mọi khách sạn đang quản lý. */
    AdminStatsDTO getStats(UUID managerId, UUID hotelId, LocalDate from, LocalDate to, String granularity);
}
