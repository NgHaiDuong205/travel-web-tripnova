package com.duong.travelweb.service.impl;

import com.duong.travelweb.converter.AdminHotelDTOConverter;
import com.duong.travelweb.converter.RoomDTOConverter;
import com.duong.travelweb.converter.RoomTypeDTOConverter;
import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.AdminHotelDTO;
import com.duong.travelweb.model.dto.HotelRequestDTO;
import com.duong.travelweb.model.dto.RoomAvailabilityDTO;
import com.duong.travelweb.model.dto.RoomAvailabilityRequestDTO;
import com.duong.travelweb.model.dto.RoomDTO;
import com.duong.travelweb.model.dto.RoomRequestDTO;
import com.duong.travelweb.model.dto.RoomTypeDTO;
import com.duong.travelweb.model.dto.RoomTypeRequestDTO;
import com.duong.travelweb.model.entity.AmenityEntity;
import com.duong.travelweb.model.entity.DestinationEntity;
import com.duong.travelweb.model.entity.HotelEntity;
import com.duong.travelweb.model.entity.RoomEntity;
import com.duong.travelweb.model.entity.RoomTypeEntity;
import com.duong.travelweb.model.entity.UserEntity;
import com.duong.travelweb.repository.AmenityRepository;
import com.duong.travelweb.repository.DestinationRepository;
import com.duong.travelweb.repository.HotelBookingRepository;
import com.duong.travelweb.repository.HotelRepository;
import com.duong.travelweb.repository.RoomAvailabilityRepository;
import com.duong.travelweb.repository.RoomRepository;
import com.duong.travelweb.repository.RoomTypeAmenityRepository;
import com.duong.travelweb.repository.RoomTypeRepository;
import com.duong.travelweb.repository.UserRepository;
import com.duong.travelweb.service.AdminHotelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminHotelServiceImpl implements AdminHotelService {
    /** Khoảng tối đa cho 1 lần xem/khoá lịch phòng. */
    private static final int MAX_AVAILABILITY_DAYS = 366;

    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeAmenityRepository roomTypeAmenityRepository;
    private final RoomAvailabilityRepository roomAvailabilityRepository;
    private final HotelBookingRepository hotelBookingRepository;
    private final DestinationRepository destinationRepository;
    private final AmenityRepository amenityRepository;
    private final UserRepository userRepository;
    private final AdminHotelDTOConverter adminHotelDTOConverter;
    private final RoomTypeDTOConverter roomTypeDTOConverter;
    private final RoomDTOConverter roomDTOConverter;

    public AdminHotelServiceImpl(HotelRepository hotelRepository,
                                 RoomTypeRepository roomTypeRepository,
                                 RoomRepository roomRepository,
                                 RoomTypeAmenityRepository roomTypeAmenityRepository,
                                 RoomAvailabilityRepository roomAvailabilityRepository,
                                 HotelBookingRepository hotelBookingRepository,
                                 DestinationRepository destinationRepository,
                                 AmenityRepository amenityRepository,
                                 UserRepository userRepository,
                                 AdminHotelDTOConverter adminHotelDTOConverter,
                                 RoomTypeDTOConverter roomTypeDTOConverter,
                                 RoomDTOConverter roomDTOConverter) {
        this.hotelRepository = hotelRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository;
        this.roomTypeAmenityRepository = roomTypeAmenityRepository;
        this.roomAvailabilityRepository = roomAvailabilityRepository;
        this.hotelBookingRepository = hotelBookingRepository;
        this.destinationRepository = destinationRepository;
        this.amenityRepository = amenityRepository;
        this.userRepository = userRepository;
        this.adminHotelDTOConverter = adminHotelDTOConverter;
        this.roomTypeDTOConverter = roomTypeDTOConverter;
        this.roomDTOConverter = roomDTOConverter;
    }

    // ---- Hotels ----

    @Override
    @Transactional(readOnly = true)
    public List<AdminHotelDTO> findHotels(String keyword, UUID destinationId, Boolean active, int page, int limit) {
        List<HotelEntity> hotels = hotelRepository.findForAdmin(normalizeKeyword(keyword), destinationId, active, page, limit);
        if (hotels.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = hotels.stream().map(HotelEntity::getId).toList();

        Map<UUID, List<String>> amenities = new HashMap<>();
        for (Object[] row : hotelRepository.findAmenityNamesByHotelIds(ids)) {
            amenities.computeIfAbsent((UUID) row[0], k -> new ArrayList<>()).add((String) row[1]);
        }
        Map<UUID, Integer> roomTypeCounts = new HashMap<>();
        for (Object[] row : roomTypeRepository.countByHotelIds(ids)) {
            roomTypeCounts.put((UUID) row[0], ((Long) row[1]).intValue());
        }
        return hotels.stream()
                .map(h -> adminHotelDTOConverter.toAdminHotelDTO(h, amenities.get(h.getId()), null,
                        roomTypeCounts.getOrDefault(h.getId(), 0)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countHotels(String keyword, UUID destinationId, Boolean active) {
        return hotelRepository.countForAdmin(normalizeKeyword(keyword), destinationId, active);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminHotelDTO getHotel(UUID hotelId) {
        return toDetailDTO(findHotel(hotelId));
    }

    @Override
    @Transactional
    public AdminHotelDTO createHotel(HotelRequestDTO request) {
        HotelEntity hotel = new HotelEntity();
        // Giá trị mặc định giống DEFAULT của bảng hotels
        hotel.setCheckInTime("14:00");
        hotel.setCheckOutTime("12:00");
        hotel.setCancellationPolicy("free");
        hotel.setCancellationHours(24);
        hotel.setBreakfastIncluded(false);
        hotel.setPetFriendly(false);
        hotel.setIsActive(true);
        hotel.setHotelAmenities(new ArrayList<>());
        LocalDateTime now = LocalDateTime.now();
        hotel.setCreatedAt(now);
        hotel.setUpdatedAt(now);

        applyHotelRequest(hotel, request);
        return toDetailDTO(hotelRepository.save(hotel));
    }

    @Override
    @Transactional
    public AdminHotelDTO updateHotel(UUID hotelId, HotelRequestDTO request) {
        HotelEntity hotel = findHotel(hotelId);
        applyHotelRequest(hotel, request);
        hotel.setUpdatedAt(LocalDateTime.now());
        return toDetailDTO(hotelRepository.save(hotel));
    }

    /** Xoá mềm: booking cũ vẫn giữ nguyên, khách sạn không còn hiện ở trang public và không đặt mới được. */
    @Override
    @Transactional
    public void deactivateHotel(UUID hotelId) {
        HotelEntity hotel = findHotel(hotelId);
        if (Boolean.TRUE.equals(hotel.getIsActive())) {
            hotel.setIsActive(false);
            hotel.setUpdatedAt(LocalDateTime.now());
            hotelRepository.save(hotel);
        }
    }

    @Override
    @Transactional
    public AdminHotelDTO updateHotelAmenities(UUID hotelId, List<UUID> amenityIds) {
        HotelEntity hotel = findHotel(hotelId);
        hotel.setHotelAmenities(loadAmenities(amenityIds));
        hotel.setUpdatedAt(LocalDateTime.now());
        return toDetailDTO(hotelRepository.save(hotel));
    }

    private void applyHotelRequest(HotelEntity hotel, HotelRequestDTO request) {
        DestinationEntity destination = destinationRepository.findById(request.getDestinationId())
                .orElseThrow(() -> ApiException.badRequest("Điểm đến không tồn tại"));
        boolean destinationChanged = hotel.getDestination() == null
                || !hotel.getDestination().getId().equals(destination.getId());
        if (destinationChanged && !Boolean.TRUE.equals(destination.getIsActive())) {
            throw ApiException.badRequest("Điểm đến đã ngừng hoạt động");
        }
        hotel.setDestination(destination);
        hotel.setName(request.getName().trim());
        hotel.setAddress(request.getAddress().trim());
        hotel.setStarRating(request.getStarRating());
        hotel.setDescription(blankToNull(request.getDescription()));
        hotel.setPhone(blankToNull(request.getPhone()));
        hotel.setEmail(blankToNull(request.getEmail()));
        hotel.setCoverImageUrl(blankToNull(request.getCoverImageUrl()));
        hotel.setLatitude(request.getLatitude());
        hotel.setLongitude(request.getLongitude());

        // Cột NOT NULL có DEFAULT: không gửi thì giữ giá trị cũ
        if (request.getCheckInTime() != null) {
            hotel.setCheckInTime(request.getCheckInTime());
        }
        if (request.getCheckOutTime() != null) {
            hotel.setCheckOutTime(request.getCheckOutTime());
        }
        if (request.getCancellationPolicy() != null) {
            hotel.setCancellationPolicy(request.getCancellationPolicy());
        }
        if (request.getCancellationHours() != null) {
            hotel.setCancellationHours(request.getCancellationHours());
        }
        if (request.getBreakfastIncluded() != null) {
            hotel.setBreakfastIncluded(request.getBreakfastIncluded());
        }
        if (request.getPetFriendly() != null) {
            hotel.setPetFriendly(request.getPetFriendly());
        }
        if (request.getIsActive() != null) {
            hotel.setIsActive(request.getIsActive());
        }

        if (request.getManagedById() == null) {
            hotel.setManagedBy(null);
        } else {
            UserEntity manager = userRepository.findById(request.getManagedById())
                    .filter(u -> u.getDeletedAt() == null)
                    .orElseThrow(() -> ApiException.badRequest("Người quản lý không tồn tại"));
            hotel.setManagedBy(manager);
        }

        // null = không đổi tiện ích
        if (request.getAmenityIds() != null) {
            hotel.setHotelAmenities(loadAmenities(request.getAmenityIds()));
        }
    }

    private AdminHotelDTO toDetailDTO(HotelEntity hotel) {
        List<String> names = new ArrayList<>();
        List<UUID> ids = new ArrayList<>();
        if (hotel.getHotelAmenities() != null) {
            for (AmenityEntity amenity : hotel.getHotelAmenities()) {
                names.add(amenity.getName());
                ids.add(amenity.getId());
            }
        }
        int roomTypeCount = 0;
        if (hotel.getId() != null) {
            for (Object[] row : roomTypeRepository.countByHotelIds(List.of(hotel.getId()))) {
                roomTypeCount = ((Long) row[1]).intValue();
            }
        }
        return adminHotelDTOConverter.toAdminHotelDTO(hotel, names, ids, roomTypeCount);
    }

    private HotelEntity findHotel(UUID hotelId) {
        return hotelRepository.findById(hotelId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy khách sạn"));
    }

    // ---- Room types ----

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypeDTO> findRoomTypes(UUID hotelId) {
        findHotel(hotelId);
        return toRoomTypeDTOs(roomTypeRepository.findAllByHotelId(hotelId));
    }

    @Override
    @Transactional
    public RoomTypeDTO createRoomType(UUID hotelId, RoomTypeRequestDTO request) {
        RoomTypeEntity roomType = new RoomTypeEntity();
        roomType.setHotel(findHotel(hotelId));
        roomType.setMaxOccupancy(2);
        roomType.setIsActive(true);
        roomType.setCreatedAt(LocalDateTime.now());
        applyRoomTypeRequest(roomType, request);
        roomType = roomTypeRepository.save(roomType);
        if (request.getAmenityIds() != null) {
            replaceRoomTypeAmenities(roomType, request.getAmenityIds());
        }
        return toRoomTypeDTOs(List.of(roomType)).get(0);
    }

    @Override
    @Transactional
    public RoomTypeDTO updateRoomType(UUID hotelId, UUID roomTypeId, RoomTypeRequestDTO request) {
        RoomTypeEntity roomType = findRoomType(hotelId, roomTypeId);
        applyRoomTypeRequest(roomType, request);
        roomType = roomTypeRepository.save(roomType);
        if (request.getAmenityIds() != null) {
            replaceRoomTypeAmenities(roomType, request.getAmenityIds());
        }
        return toRoomTypeDTOs(List.of(roomType)).get(0);
    }

    /** Xoá mềm: phòng và booking cũ giữ nguyên, hạng phòng không còn đặt được. */
    @Override
    @Transactional
    public void deactivateRoomType(UUID hotelId, UUID roomTypeId) {
        RoomTypeEntity roomType = findRoomType(hotelId, roomTypeId);
        if (Boolean.TRUE.equals(roomType.getIsActive())) {
            roomType.setIsActive(false);
            roomTypeRepository.save(roomType);
        }
    }

    private void applyRoomTypeRequest(RoomTypeEntity roomType, RoomTypeRequestDTO request) {
        roomType.setName(request.getName().trim());
        roomType.setDescription(blankToNull(request.getDescription()));
        roomType.setBedType(blankToNull(request.getBedType()));
        roomType.setAreaSqM(request.getAreaSqM());
        roomType.setPricePerNight(request.getPricePerNight());
        roomType.setCoverImageUrl(blankToNull(request.getCoverImageUrl()));
        if (request.getMaxOccupancy() != null) {
            roomType.setMaxOccupancy(request.getMaxOccupancy());
        }
        if (request.getIsActive() != null) {
            roomType.setIsActive(request.getIsActive());
        }
    }

    private void replaceRoomTypeAmenities(RoomTypeEntity roomType, List<UUID> amenityIds) {
        List<AmenityEntity> amenities = loadAmenities(amenityIds);
        roomTypeAmenityRepository.deleteByRoomTypeId(roomType.getId());
        for (AmenityEntity amenity : amenities) {
            roomTypeAmenityRepository.insertLink(roomType.getId(), amenity.getId());
        }
    }

    private List<RoomTypeDTO> toRoomTypeDTOs(List<RoomTypeEntity> roomTypes) {
        if (roomTypes.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = roomTypes.stream().map(RoomTypeEntity::getId).toList();
        Map<UUID, Integer> roomCounts = new HashMap<>();
        for (Object[] row : roomTypeRepository.countRoomsByRoomTypeIds(ids)) {
            roomCounts.put((UUID) row[0], ((Long) row[1]).intValue());
        }
        Map<UUID, List<String>> amenityNames = new HashMap<>();
        Map<UUID, List<UUID>> amenityIds = new HashMap<>();
        for (Object[] row : roomTypeAmenityRepository.findAmenitiesByRoomTypeIds(ids)) {
            UUID roomTypeId = (UUID) row[0];
            amenityIds.computeIfAbsent(roomTypeId, k -> new ArrayList<>()).add((UUID) row[1]);
            amenityNames.computeIfAbsent(roomTypeId, k -> new ArrayList<>()).add((String) row[2]);
        }
        List<RoomTypeDTO> result = new ArrayList<>();
        for (RoomTypeEntity roomType : roomTypes) {
            RoomTypeDTO dto = roomTypeDTOConverter.toRoomTypeDTO(roomType,
                    roomCounts.getOrDefault(roomType.getId(), 0), amenityNames.get(roomType.getId()));
            dto.setIsActive(roomType.getIsActive());
            dto.setAmenityIds(amenityIds.getOrDefault(roomType.getId(), new ArrayList<>()));
            result.add(dto);
        }
        return result;
    }

    private RoomTypeEntity findRoomType(UUID hotelId, UUID roomTypeId) {
        return roomTypeRepository.findByIdAndHotelId(roomTypeId, hotelId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hạng phòng"));
    }

    // ---- Rooms ----

    @Override
    @Transactional(readOnly = true)
    public List<RoomDTO> findRooms(UUID hotelId, UUID roomTypeId) {
        findHotel(hotelId);
        List<RoomEntity> rooms = roomRepository.findAllByHotelIdForAdmin(hotelId, roomTypeId);
        if (rooms.isEmpty()) {
            return List.of();
        }
        Set<UUID> roomTypeIds = new LinkedHashSet<>();
        for (RoomEntity room : rooms) {
            roomTypeIds.add(room.getRoomType().getId());
        }
        Map<UUID, List<String>> amenities = new HashMap<>();
        for (Object[] row : roomRepository.findAmenityNamesByRoomTypeIds(new ArrayList<>(roomTypeIds))) {
            amenities.computeIfAbsent((UUID) row[0], k -> new ArrayList<>()).add((String) row[1]);
        }
        return rooms.stream()
                .map(r -> roomDTOConverter.toRoomDTO(r, amenities.getOrDefault(r.getRoomType().getId(), new ArrayList<>())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoomDTO getRoom(UUID hotelId, UUID roomId) {
        return toRoomDTO(findRoom(hotelId, roomId));
    }

    @Override
    @Transactional
    public RoomDTO createRoom(UUID hotelId, RoomRequestDTO request) {
        RoomTypeEntity roomType = findRoomType(hotelId, request.getRoomTypeId());
        String roomNumber = request.getRoomNumber().trim();
        if (roomRepository.existsRoomNumberInHotel(hotelId, roomNumber, null)) {
            throw ApiException.conflict("Số phòng " + roomNumber + " đã tồn tại trong khách sạn");
        }
        RoomEntity room = new RoomEntity();
        room.setRoomType(roomType);
        room.setRoomNumber(roomNumber);
        room.setFloor(request.getFloor());
        room.setStatus(request.getStatus() != null ? request.getStatus() : "available");
        return toRoomDTO(roomRepository.save(room));
    }

    @Override
    @Transactional
    public RoomDTO updateRoom(UUID hotelId, UUID roomId, RoomRequestDTO request) {
        RoomEntity room = findRoom(hotelId, roomId);
        String roomNumber = request.getRoomNumber().trim();
        if (roomRepository.existsRoomNumberInHotel(hotelId, roomNumber, roomId)) {
            throw ApiException.conflict("Số phòng " + roomNumber + " đã tồn tại trong khách sạn");
        }
        if (!room.getRoomType().getId().equals(request.getRoomTypeId())) {
            RoomTypeEntity newType = findRoomType(hotelId, request.getRoomTypeId());
            // Booking gắn với hạng phòng — đổi hạng khi còn booking mở sẽ làm lệch giá/sức chứa
            if (hotelBookingRepository.existsOpenBookingForRoom(roomId, LocalDate.now())) {
                throw ApiException.conflict("Phòng còn booking chưa kết thúc, không thể đổi hạng phòng");
            }
            room.setRoomType(newType);
        }
        room.setRoomNumber(roomNumber);
        room.setFloor(request.getFloor());
        if (request.getStatus() != null) {
            room.setStatus(request.getStatus());
        }
        return toRoomDTO(roomRepository.save(room));
    }

    /** Chỉ xoá cứng phòng chưa từng được đặt; phòng có lịch sử thì chuyển sang maintenance. */
    @Override
    @Transactional
    public void deleteRoom(UUID hotelId, UUID roomId) {
        RoomEntity room = findRoom(hotelId, roomId);
        if (hotelBookingRepository.existsByRoomId(roomId) || roomAvailabilityRepository.existsBookedDay(roomId)) {
            throw ApiException.conflict("Phòng đã có lịch sử đặt, hãy chuyển sang trạng thái maintenance thay vì xoá");
        }
        roomRepository.delete(room);
    }

    private RoomDTO toRoomDTO(RoomEntity room) {
        List<String> amenities = new ArrayList<>();
        for (Object[] row : roomRepository.findAmenityNamesByRoomTypeIds(List.of(room.getRoomType().getId()))) {
            amenities.add((String) row[1]);
        }
        return roomDTOConverter.toRoomDTO(room, amenities);
    }

    private RoomEntity findRoom(UUID hotelId, UUID roomId) {
        return roomRepository.findByIdAndHotelId(roomId, hotelId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy phòng"));
    }

    // ---- Room availability ----

    @Override
    @Transactional(readOnly = true)
    public List<RoomAvailabilityDTO> getRoomAvailability(UUID roomId, LocalDate from, LocalDate to) {
        if (!roomRepository.existsById(roomId)) {
            throw ApiException.notFound("Không tìm thấy phòng");
        }
        validateRange(from, to);
        return roomAvailabilityRepository.findUnavailableInRange(roomId, from, to).stream()
                .map(ra -> new RoomAvailabilityDTO(ra.getDate(), ra.getStatus(), ra.getBlockedReason()))
                .toList();
    }

    @Override
    @Transactional
    public List<RoomAvailabilityDTO> updateRoomAvailability(UUID roomId, RoomAvailabilityRequestDTO request) {
        // Khoá dòng phòng: tuần tự với luồng xác nhận booking (HotelBookingServiceImpl cũng lock phòng)
        roomRepository.lockById(roomId).orElseThrow(() -> ApiException.notFound("Không tìm thấy phòng"));
        LocalDate from = request.getFrom();
        LocalDate to = request.getTo();
        validateRange(from, to);

        if ("blocked".equals(request.getStatus())) {
            if (from.isBefore(LocalDate.now())) {
                throw ApiException.badRequest("Không thể khoá ngày trong quá khứ");
            }
            long bookedDays = roomAvailabilityRepository.countBookedInRange(roomId, from, to);
            if (bookedDays > 0) {
                throw ApiException.conflict("Có " + bookedDays + " ngày trong khoảng này đã được đặt");
            }
            if (hotelBookingRepository.existsOpenBookingInRange(roomId, from, to.plusDays(1))) {
                throw ApiException.conflict("Phòng đang được giữ/đặt trong khoảng này");
            }
            String reason = blankToNull(request.getReason());
            for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
                roomAvailabilityRepository.blockDay(roomId, date, reason);
            }
        } else {
            roomAvailabilityRepository.unblockDays(roomId, from, to);
        }
        return roomAvailabilityRepository.findUnavailableInRange(roomId, from, to).stream()
                .map(ra -> new RoomAvailabilityDTO(ra.getDate(), ra.getStatus(), ra.getBlockedReason()))
                .toList();
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw ApiException.badRequest("Thiếu khoảng ngày");
        }
        if (to.isBefore(from)) {
            throw ApiException.badRequest("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_AVAILABILITY_DAYS) {
            throw ApiException.badRequest("Khoảng ngày tối đa " + MAX_AVAILABILITY_DAYS + " ngày");
        }
    }

    // ---- Helpers ----

    private List<AmenityEntity> loadAmenities(List<UUID> amenityIds) {
        Set<UUID> unique = new LinkedHashSet<>(amenityIds);
        unique.remove(null);
        if (unique.isEmpty()) {
            return new ArrayList<>();
        }
        List<AmenityEntity> amenities = amenityRepository.findAllById(unique);
        if (amenities.size() != unique.size()) {
            throw ApiException.badRequest("Có tiện ích không tồn tại");
        }
        return new ArrayList<>(amenities);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
