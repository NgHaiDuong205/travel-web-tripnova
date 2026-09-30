package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
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
import com.duong.travelweb.model.entity.HotelEntity;
import com.duong.travelweb.repository.HotelRepository;
import com.duong.travelweb.repository.RoomRepository;
import com.duong.travelweb.service.AdminHotelService;
import com.duong.travelweb.service.AdminStatsService;
import com.duong.travelweb.service.HotelImageService;
import com.duong.travelweb.service.ManagedHotelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Lớp kiểm tra quyền cho trang quản lý khách sạn; nghiệp vụ dùng lại AdminHotelService / HotelImageService
 * (các hàm đó đã tìm hạng phòng / phòng / ảnh theo cặp (hotelId, id) nên chỉ cần kiểm tra hotelId).
 */
@Service
public class ManagedHotelServiceImpl implements ManagedHotelService {
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final AdminHotelService adminHotelService;
    private final HotelImageService hotelImageService;
    private final AdminStatsService adminStatsService;

    public ManagedHotelServiceImpl(HotelRepository hotelRepository,
                                   RoomRepository roomRepository,
                                   AdminHotelService adminHotelService,
                                   HotelImageService hotelImageService,
                                   AdminStatsService adminStatsService) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.adminHotelService = adminHotelService;
        this.hotelImageService = hotelImageService;
        this.adminStatsService = adminStatsService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminHotelDTO> findHotels(UUID managerId) {
        return hotelRepository.findByManagerId(managerId).stream()
                .map(hotel -> adminHotelService.getHotel(hotel.getId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminHotelDTO getHotel(UUID managerId, UUID hotelId) {
        requireHotel(managerId, hotelId);
        return adminHotelService.getHotel(hotelId);
    }

    @Override
    @Transactional
    public AdminHotelDTO updateHotel(UUID managerId, UUID hotelId, HotelRequestDTO request) {
        requireHotel(managerId, hotelId);
        AdminHotelDTO current = adminHotelService.getHotel(hotelId);
        // Không đổi được người quản lý và trạng thái hiển thị (bật lại khách sạn đã ẩn là việc của admin).
        request.setManagedById(managerId);
        request.setIsActive(current.getIsActive());
        return adminHotelService.updateHotel(hotelId, request);
    }

    @Override
    @Transactional
    public void deactivateHotel(UUID managerId, UUID hotelId) {
        requireHotel(managerId, hotelId);
        adminHotelService.deactivateHotel(hotelId);
    }

    @Override
    @Transactional
    public AdminHotelDTO updateHotelAmenities(UUID managerId, UUID hotelId, List<UUID> amenityIds) {
        requireHotel(managerId, hotelId);
        return adminHotelService.updateHotelAmenities(hotelId, amenityIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypeDTO> findRoomTypes(UUID managerId, UUID hotelId) {
        requireHotel(managerId, hotelId);
        return adminHotelService.findRoomTypes(hotelId);
    }

    @Override
    @Transactional
    public RoomTypeDTO createRoomType(UUID managerId, UUID hotelId, RoomTypeRequestDTO request) {
        requireHotel(managerId, hotelId);
        return adminHotelService.createRoomType(hotelId, request);
    }

    @Override
    @Transactional
    public RoomTypeDTO updateRoomType(UUID managerId, UUID hotelId, UUID roomTypeId, RoomTypeRequestDTO request) {
        requireHotel(managerId, hotelId);
        return adminHotelService.updateRoomType(hotelId, roomTypeId, request);
    }

    @Override
    @Transactional
    public void deactivateRoomType(UUID managerId, UUID hotelId, UUID roomTypeId) {
        requireHotel(managerId, hotelId);
        adminHotelService.deactivateRoomType(hotelId, roomTypeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomDTO> findRooms(UUID managerId, UUID hotelId, UUID roomTypeId) {
        requireHotel(managerId, hotelId);
        return adminHotelService.findRooms(hotelId, roomTypeId);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomDTO getRoom(UUID managerId, UUID hotelId, UUID roomId) {
        requireHotel(managerId, hotelId);
        return adminHotelService.getRoom(hotelId, roomId);
    }

    @Override
    @Transactional
    public RoomDTO createRoom(UUID managerId, UUID hotelId, RoomRequestDTO request) {
        requireHotel(managerId, hotelId);
        return adminHotelService.createRoom(hotelId, request);
    }

    @Override
    @Transactional
    public RoomDTO updateRoom(UUID managerId, UUID hotelId, UUID roomId, RoomRequestDTO request) {
        requireHotel(managerId, hotelId);
        return adminHotelService.updateRoom(hotelId, roomId, request);
    }

    @Override
    @Transactional
    public void deleteRoom(UUID managerId, UUID hotelId, UUID roomId) {
        requireHotel(managerId, hotelId);
        adminHotelService.deleteRoom(hotelId, roomId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomAvailabilityDTO> getRoomAvailability(UUID managerId, UUID roomId, LocalDate from, LocalDate to) {
        requireRoom(managerId, roomId);
        return adminHotelService.getRoomAvailability(roomId, from, to);
    }

    @Override
    @Transactional
    public List<RoomAvailabilityDTO> updateRoomAvailability(UUID managerId, UUID roomId, RoomAvailabilityRequestDTO request) {
        requireRoom(managerId, roomId);
        return adminHotelService.updateRoomAvailability(roomId, request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelImageDTO> findImages(UUID managerId, UUID hotelId) {
        requireHotel(managerId, hotelId);
        return hotelImageService.findByHotel(hotelId, false);
    }

    @Override
    @Transactional
    public HotelImageDTO addImage(UUID managerId, UUID hotelId, HotelImageRequestDTO request) {
        requireHotel(managerId, hotelId);
        return hotelImageService.add(managerId, hotelId, request);
    }

    @Override
    @Transactional
    public HotelImageDTO updateImage(UUID managerId, UUID hotelId, UUID imageId, HotelImageRequestDTO request) {
        requireHotel(managerId, hotelId);
        return hotelImageService.update(hotelId, imageId, request);
    }

    @Override
    @Transactional
    public HotelImageDTO setCoverImage(UUID managerId, UUID hotelId, UUID imageId) {
        requireHotel(managerId, hotelId);
        return hotelImageService.setCover(hotelId, imageId);
    }

    @Override
    @Transactional
    public void deleteImage(UUID managerId, UUID hotelId, UUID imageId) {
        requireHotel(managerId, hotelId);
        hotelImageService.delete(hotelId, imageId);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStatsDTO getStats(UUID managerId, UUID hotelId, LocalDate from, LocalDate to, String granularity) {
        List<UUID> hotelIds;
        if (hotelId != null) {
            requireHotel(managerId, hotelId);
            hotelIds = List.of(hotelId);
        } else {
            hotelIds = hotelRepository.findByManagerId(managerId).stream().map(HotelEntity::getId).toList();
        }
        return adminStatsService.getHotelStats(hotelIds, from, to, granularity);
    }

    /** 404 (không phải 403) để không lộ khách sạn của người khác có tồn tại hay không. */
    private void requireHotel(UUID managerId, UUID hotelId) {
        if (hotelId == null || !hotelRepository.isManagedBy(hotelId, managerId)) {
            throw ApiException.notFound("Không tìm thấy khách sạn");
        }
    }

    private void requireRoom(UUID managerId, UUID roomId) {
        UUID hotelId = roomRepository.findHotelIdByRoomId(roomId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy phòng"));
        if (!hotelRepository.isManagedBy(hotelId, managerId)) {
            throw ApiException.notFound("Không tìm thấy phòng");
        }
    }
}
