package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.AdminHotelDTO;
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

/** Quản trị khách sạn, hạng phòng, phòng và lịch khoá phòng. */
public interface AdminHotelService {
    /** managedById != null -> chỉ khách sạn do user đó quản lý. */
    List<AdminHotelDTO> findHotels(String keyword, UUID destinationId, Boolean active, UUID managedById, int page, int limit);
    long countHotels(String keyword, UUID destinationId, Boolean active, UUID managedById);
    AdminHotelDTO getHotel(UUID hotelId);
    AdminHotelDTO createHotel(HotelRequestDTO request);
    AdminHotelDTO updateHotel(UUID hotelId, HotelRequestDTO request);
    void deactivateHotel(UUID hotelId);
    AdminHotelDTO updateHotelAmenities(UUID hotelId, List<UUID> amenityIds);

    List<RoomTypeDTO> findRoomTypes(UUID hotelId);
    RoomTypeDTO createRoomType(UUID hotelId, RoomTypeRequestDTO request);
    RoomTypeDTO updateRoomType(UUID hotelId, UUID roomTypeId, RoomTypeRequestDTO request);
    void deactivateRoomType(UUID hotelId, UUID roomTypeId);

    List<RoomDTO> findRooms(UUID hotelId, UUID roomTypeId);
    RoomDTO getRoom(UUID hotelId, UUID roomId);
    RoomDTO createRoom(UUID hotelId, RoomRequestDTO request);
    RoomDTO updateRoom(UUID hotelId, UUID roomId, RoomRequestDTO request);
    void deleteRoom(UUID hotelId, UUID roomId);

    List<RoomAvailabilityDTO> getRoomAvailability(UUID roomId, LocalDate from, LocalDate to);
    List<RoomAvailabilityDTO> updateRoomAvailability(UUID roomId, RoomAvailabilityRequestDTO request);
}
