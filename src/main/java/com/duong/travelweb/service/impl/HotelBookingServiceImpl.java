package com.duong.travelweb.service.impl;

import com.duong.travelweb.converter.HotelBookingDTOConverter;
import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.HotelBookingCreatedDTO;
import com.duong.travelweb.model.dto.HotelBookingDTO;
import com.duong.travelweb.model.dto.HotelBookingRequestDTO;
import com.duong.travelweb.model.dto.RoomAvailabilityCheckDTO;
import com.duong.travelweb.model.entity.HotelBookingEntity;
import com.duong.travelweb.model.entity.HotelEntity;
import com.duong.travelweb.model.entity.OrderEntity;
import com.duong.travelweb.model.entity.PaymentEntity;
import com.duong.travelweb.model.entity.RoomEntity;
import com.duong.travelweb.model.entity.RoomTypeEntity;
import com.duong.travelweb.model.entity.UserEntity;
import com.duong.travelweb.repository.HotelBookingRepository;
import com.duong.travelweb.repository.OrderRepository;
import com.duong.travelweb.repository.PaymentRepository;
import com.duong.travelweb.repository.RoomAvailabilityRepository;
import com.duong.travelweb.repository.RoomRepository;
import com.duong.travelweb.repository.RoomTypeRepository;
import com.duong.travelweb.repository.UserRepository;
import com.duong.travelweb.service.HotelBookingService;
import com.duong.travelweb.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class HotelBookingServiceImpl implements HotelBookingService {
    public static final String EXPIRED_REASON = "Hết thời gian giữ phòng, đơn chưa được thanh toán";
    private static final Logger log = LoggerFactory.getLogger(HotelBookingServiceImpl.class);
    private static final int MAX_NIGHTS = 30;
    private static final int DEFAULT_CANCELLATION_HOURS = 24;
    private static final LocalTime DEFAULT_CHECK_IN_TIME = LocalTime.of(14, 0);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final HotelBookingRepository hotelBookingRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomAvailabilityRepository roomAvailabilityRepository;
    private final UserRepository userRepository;
    private final HotelBookingDTOConverter hotelBookingDTOConverter;
    private final long holdMinutes;
    private final String currencyCode;
    private final String frontendUrl;

    public HotelBookingServiceImpl(HotelBookingRepository hotelBookingRepository,
                                   OrderRepository orderRepository,
                                   PaymentRepository paymentRepository,
                                   RoomRepository roomRepository,
                                   RoomTypeRepository roomTypeRepository,
                                   RoomAvailabilityRepository roomAvailabilityRepository,
                                   UserRepository userRepository,
                                   HotelBookingDTOConverter hotelBookingDTOConverter,
                                   @Value("${app.booking.hold-minutes:15}") long holdMinutes,
                                   @Value("${app.booking.currency:USD}") String currencyCode,
                                   @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl) {
        this.hotelBookingRepository = hotelBookingRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomAvailabilityRepository = roomAvailabilityRepository;
        this.userRepository = userRepository;
        this.hotelBookingDTOConverter = hotelBookingDTOConverter;
        this.holdMinutes = holdMinutes;
        this.currencyCode = currencyCode;
        this.frontendUrl = frontendUrl;
    }

    @Override
    @Transactional(readOnly = true)
    public RoomAvailabilityCheckDTO checkAvailability(UUID hotelId, UUID roomTypeId, UUID roomId,
                                                      LocalDate checkIn, LocalDate checkOut) {
        int nights = validateDates(checkIn, checkOut);
        RoomTypeEntity roomType = findActiveRoomType(hotelId, roomTypeId);
        List<RoomEntity> rooms = findBookableRooms(roomTypeId, checkIn, checkOut, null);

        RoomAvailabilityCheckDTO dto = new RoomAvailabilityCheckDTO();
        dto.setHotelId(hotelId);
        dto.setRoomTypeId(roomTypeId);
        dto.setRoomId(roomId);
        dto.setCheckIn(checkIn);
        dto.setCheckOut(checkOut);
        dto.setNights(nights);
        dto.setAvailableRooms(rooms.size());
        dto.setAvailable(roomId == null
                ? !rooms.isEmpty()
                : rooms.stream().anyMatch(r -> r.getId().equals(roomId)));
        BigDecimal pricePerNight = priceOf(roomType);
        dto.setPricePerNight(pricePerNight);
        dto.setTotalPrice(pricePerNight.multiply(BigDecimal.valueOf(nights)));
        return dto;
    }

    @Override
    @Transactional
    public HotelBookingCreatedDTO createBooking(UUID userId, HotelBookingRequestDTO request) {
        int nights = validateDates(request.getCheckIn(), request.getCheckOut());
        RoomTypeEntity roomType = findActiveRoomType(request.getHotelId(), request.getRoomTypeId());
        HotelEntity hotel = roomType.getHotel();
        if (!Boolean.TRUE.equals(hotel.getIsActive())) {
            throw ApiException.badRequest("Khách sạn hiện không nhận đặt phòng");
        }
        int children = request.getChildren() != null ? request.getChildren() : 0;
        if (roomType.getMaxOccupancy() != null && request.getAdults() + children > roomType.getMaxOccupancy()) {
            throw ApiException.badRequest("Số khách vượt quá sức chứa tối đa " + roomType.getMaxOccupancy() + " người của loại phòng");
        }

        List<RoomEntity> freeRooms = findBookableRooms(roomType.getId(), request.getCheckIn(), request.getCheckOut(), null);
        RoomEntity room;
        if (request.getRoomId() != null) {
            room = freeRooms.stream()
                    .filter(r -> r.getId().equals(request.getRoomId()))
                    .findFirst()
                    .orElseThrow(() -> ApiException.conflict("Phòng đã được đặt trong khoảng thời gian này"));
        } else {
            room = freeRooms.stream()
                    .findFirst()
                    .orElseThrow(() -> ApiException.conflict("Loại phòng này đã hết phòng trống trong khoảng thời gian đã chọn"));
        }

        UserEntity user = userRepository.getReferenceById(userId);
        LocalDateTime now = LocalDateTime.now();
        BigDecimal total = priceOf(roomType).multiply(BigDecimal.valueOf(nights));

        OrderEntity order = new OrderEntity();
        order.setUser(user);
        order.setOrderCode(generateOrderCode());
        order.setSubtotal(total);
        order.setDiscountTotal(BigDecimal.ZERO);
        order.setTaxAmount(BigDecimal.ZERO);
        order.setTotalAmount(total);
        order.setCurrencyCode(currencyCode);
        order.setLoyaltyPointsUsed(0);
        order.setLoyaltyPointsEarned(0);
        order.setStatus("pending");
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order = orderRepository.save(order);

        HotelBookingEntity booking = new HotelBookingEntity();
        booking.setOrder(order);
        booking.setUser(user);
        booking.setHotel(hotel);
        booking.setRoomType(roomType);
        booking.setRoom(room);
        booking.setCheckInDate(request.getCheckIn());
        booking.setCheckOutDate(request.getCheckOut());
        booking.setNumNights(nights);
        booking.setNumAdults(request.getAdults());
        booking.setNumChildren(children);
        booking.setTotalPrice(total);
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setSpecialRequests(request.getSpecialRequests() != null && !request.getSpecialRequests().isBlank()
                ? request.getSpecialRequests().trim() : null);
        booking.setStatus("pending");
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);
        booking = hotelBookingRepository.save(booking);

        PaymentEntity payment = new PaymentEntity();
        payment.setOrder(order);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setAmount(total);
        payment.setCurrencyCode(currencyCode);
        payment.setStatus("pending");
        payment.setCreatedAt(now);
        payment = paymentRepository.save(payment);

        HotelBookingCreatedDTO dto = new HotelBookingCreatedDTO();
        dto.setOrderId(order.getId());
        dto.setOrderCode(order.getOrderCode());
        dto.setBookingId(booking.getId());
        dto.setPaymentId(payment.getId());
        dto.setAmount(total);
        dto.setCurrencyCode(currencyCode);
        dto.setPaymentUrl(frontendUrl + "/payment/" + payment.getId());
        dto.setHoldExpiresAt(now.plusMinutes(holdMinutes));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public HotelBookingDTO getBooking(UUID userId, UUID bookingId) {
        HotelBookingEntity booking = findOwnedBooking(userId, bookingId);
        return toDTO(booking);
    }

    @Override
    @Transactional
    public HotelBookingDTO cancelBooking(UUID userId, UUID bookingId, String reason) {
        HotelBookingEntity booking = findOwnedBooking(userId, bookingId);
        OrderEntity order = booking.getOrder();
        String cancelReason = reason != null && !reason.isBlank() ? reason.trim() : "Khách hàng huỷ đặt phòng";
        LocalDateTime now = LocalDateTime.now();

        switch (booking.getStatus()) {
            case "pending" -> cancelPendingOrder(order, cancelReason);
            case "confirmed" -> {
                if (!isFreeCancellation(booking, now)) {
                    throw ApiException.badRequest("Đã quá hạn huỷ miễn phí của khách sạn, không thể huỷ đặt phòng");
                }
                refundConfirmedBooking(booking, cancelReason, now);
            }
            default -> throw ApiException.badRequest("Không thể huỷ đặt phòng ở trạng thái " + booking.getStatus());
        }
        return toDTO(booking);
    }

    /** Huỷ booking đã xác nhận: trả phòng, hoàn 100%, cập nhật order/payment. */
    private void refundConfirmedBooking(HotelBookingEntity booking, String reason, LocalDateTime now) {
        OrderEntity order = booking.getOrder();
        if (booking.getRoom() != null) {
            roomAvailabilityRepository.releaseDays(booking.getRoom().getId(),
                    booking.getCheckInDate(), booking.getCheckOutDate());
        }
        booking.setStatus("cancelled");
        booking.setRefundAmount(booking.getTotalPrice());
        booking.setRefundReason(reason);
        booking.setRefundedAt(now);
        booking.setUpdatedAt(now);
        order.setStatus("refunded");
        order.setCancelledAt(now);
        order.setCancelReason(reason);
        order.setUpdatedAt(now);
        PaymentEntity payment = findLatestPayment(order);
        if (payment != null && "success".equals(payment.getStatus())) {
            payment.setStatus("refunded");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelBookingDTO> findForAdmin(String status, String keyword, int page, int limit) {
        return toDTOs(hotelBookingRepository.findForAdmin(blankToNull(status), blankToNull(keyword), page, limit));
    }

    @Override
    @Transactional(readOnly = true)
    public long countForAdmin(String status, String keyword) {
        return hotelBookingRepository.countForAdmin(blankToNull(status), blankToNull(keyword));
    }

    @Override
    @Transactional
    public HotelBookingDTO updateStatusByAdmin(UUID bookingId, String newStatus, String reason) {
        HotelBookingEntity booking = hotelBookingRepository.findDetailById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đặt phòng"));
        String current = booking.getStatus();
        LocalDateTime now = LocalDateTime.now();
        String note = reason != null && !reason.isBlank() ? reason.trim() : "Quản trị viên huỷ đặt phòng";

        switch (current + "->" + newStatus) {
            case "pending->cancelled" -> cancelPendingOrder(booking.getOrder(), note);
            case "confirmed->cancelled" -> refundConfirmedBooking(booking, note, now);
            case "confirmed->checked_in" -> {
                booking.setStatus("checked_in");
                if (booking.getRoom() != null) {
                    booking.getRoom().setStatus("occupied");
                }
            }
            case "checked_in->checked_out" -> {
                booking.setStatus("checked_out");
                if (booking.getRoom() != null) {
                    booking.getRoom().setStatus("available");
                }
            }
            case "checked_out->completed", "confirmed->no_show" -> booking.setStatus(newStatus);
            default -> throw ApiException.badRequest("Không thể chuyển trạng thái từ " + current + " sang " + newStatus);
        }
        booking.setUpdatedAt(now);
        return toDTO(booking);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() || "all".equals(value) ? null : value.trim();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelBookingDTO> findMyBookings(UUID userId, String statusGroup, int page, int limit) {
        List<HotelBookingEntity> bookings = hotelBookingRepository.findByUser(userId, normalizeGroup(statusGroup), page, limit);
        return toDTOs(bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public long countMyBookings(UUID userId, String statusGroup) {
        return hotelBookingRepository.countByUser(userId, normalizeGroup(statusGroup));
    }

    /** Chuyển danh sách booking sang DTO, lấy payment mới nhất theo lô (tránh N+1). */
    private List<HotelBookingDTO> toDTOs(List<HotelBookingEntity> bookings) {
        if (bookings.isEmpty()) {
            return List.of();
        }
        List<UUID> orderIds = bookings.stream().map(b -> b.getOrder().getId()).distinct().toList();
        Map<UUID, PaymentEntity> latestPaymentByOrder = new HashMap<>();
        for (PaymentEntity payment : paymentRepository.findByOrderIds(orderIds)) {
            latestPaymentByOrder.putIfAbsent(payment.getOrder().getId(), payment);
        }
        LocalDateTime now = LocalDateTime.now();
        return bookings.stream()
                .map(b -> hotelBookingDTOConverter.toHotelBookingDTO(b,
                        latestPaymentByOrder.get(b.getOrder().getId()), isCancellable(b, now)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HotelBookingDTO findNextUpcoming(UUID userId) {
        List<HotelBookingEntity> upcoming = hotelBookingRepository.findUpcomingConfirmed(
                userId, LocalDate.now(), PageRequest.of(0, 1));
        return upcoming.isEmpty() ? null : toDTOs(upcoming).get(0);
    }

    private String normalizeGroup(String statusGroup) {
        if (statusGroup == null || statusGroup.isBlank()) {
            return null;
        }
        return switch (statusGroup) {
            case "upcoming", "completed", "cancelled", "pending" -> statusGroup;
            case "all" -> null;
            default -> throw ApiException.badRequest("Trạng thái lọc không hợp lệ: " + statusGroup);
        };
    }

    @Override
    @Transactional
    public boolean confirmOrder(OrderEntity order) {
        LocalDateTime now = LocalDateTime.now();
        List<HotelBookingEntity> bookings = hotelBookingRepository.findByOrderId(order.getId());
        boolean allConfirmed = true;

        for (HotelBookingEntity booking : bookings) {
            if ("confirmed".equals(booking.getStatus())) {
                continue;
            }
            RoomEntity room = lockFreeRoom(booking);
            if (room == null) {
                allConfirmed = false;
                booking.setStatus("refunded");
                booking.setRefundAmount(booking.getTotalPrice());
                booking.setRefundReason("Phòng đã được khách khác đặt trước khi thanh toán hoàn tất");
                booking.setRefundedAt(now);
                booking.setUpdatedAt(now);
                continue;
            }
            for (LocalDate day = booking.getCheckInDate(); day.isBefore(booking.getCheckOutDate()); day = day.plusDays(1)) {
                if (roomAvailabilityRepository.bookDay(room.getId(), day) != 1) {
                    throw new IllegalStateException("Không khoá được phòng " + room.getId() + " ngày " + day);
                }
            }
            booking.setRoom(room);
            booking.setStatus("confirmed");
            booking.setUpdatedAt(now);
        }

        order.setStatus(allConfirmed ? "paid" : "refunded");
        if (!allConfirmed) {
            order.setCancelledAt(now);
            order.setCancelReason("Hết phòng, đã hoàn tiền");
        } else {
            order.setCancelledAt(null);
            order.setCancelReason(null);
        }
        order.setUpdatedAt(now);
        return allConfirmed;
    }

    @Override
    @Transactional
    public void cancelPendingOrder(OrderEntity order, String reason) {
        LocalDateTime now = LocalDateTime.now();
        for (HotelBookingEntity booking : hotelBookingRepository.findByOrderId(order.getId())) {
            if ("pending".equals(booking.getStatus())) {
                booking.setStatus("cancelled");
                booking.setUpdatedAt(now);
            }
        }
        order.setStatus("cancelled");
        order.setCancelledAt(now);
        order.setCancelReason(reason);
        order.setUpdatedAt(now);
        PaymentEntity payment = findLatestPayment(order);
        if (payment != null && "pending".equals(payment.getStatus())) {
            payment.setStatus("failed");
        }
    }

    @Override
    @Transactional
    public int expirePendingBookings() {
        List<HotelBookingEntity> expired = hotelBookingRepository.findPendingCreatedBefore(
                LocalDateTime.now().minusMinutes(holdMinutes));
        for (HotelBookingEntity booking : expired) {
            cancelPendingOrder(booking.getOrder(), EXPIRED_REASON);
        }
        if (!expired.isEmpty()) {
            log.info("Expired {} pending hotel bookings", expired.size());
        }
        return expired.size();
    }

    /** Khoá phòng đã chọn; nếu phòng bị người khác lấy mất thì thử phòng khác cùng loại. */
    private RoomEntity lockFreeRoom(HotelBookingEntity booking) {
        if (booking.getRoom() != null) {
            RoomEntity room = roomRepository.lockById(booking.getRoom().getId()).orElse(null);
            if (room != null && isRoomFree(room, booking)) {
                return room;
            }
        }
        for (RoomEntity candidate : findBookableRooms(booking.getRoomType().getId(),
                booking.getCheckInDate(), booking.getCheckOutDate(), booking.getId())) {
            RoomEntity room = roomRepository.lockById(candidate.getId()).orElse(null);
            if (room != null && isRoomFree(room, booking)) {
                return room;
            }
        }
        return null;
    }

    private boolean isRoomFree(RoomEntity room, HotelBookingEntity booking) {
        return !"maintenance".equals(room.getStatus())
                && roomAvailabilityRepository.countUnavailableDays(room.getId(),
                        booking.getCheckInDate(), booking.getCheckOutDate()) == 0;
    }

    private List<RoomEntity> findBookableRooms(UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, UUID excludeBookingId) {
        return roomRepository.findBookableRooms(roomTypeId, checkIn, checkOut,
                LocalDateTime.now().minusMinutes(holdMinutes), excludeBookingId);
    }

    private boolean isFreeCancellation(HotelBookingEntity booking, LocalDateTime now) {
        HotelEntity hotel = booking.getHotel();
        int hours = hotel.getCancellationHours() != null ? hotel.getCancellationHours() : DEFAULT_CANCELLATION_HOURS;
        LocalTime checkInTime = DEFAULT_CHECK_IN_TIME;
        if (hotel.getCheckInTime() != null) {
            try {
                checkInTime = LocalTime.parse(hotel.getCheckInTime());
            } catch (RuntimeException ignored) {
                // giữ giờ mặc định 14:00
            }
        }
        return now.isBefore(booking.getCheckInDate().atTime(checkInTime).minusHours(hours));
    }

    private HotelBookingEntity findOwnedBooking(UUID userId, UUID bookingId) {
        HotelBookingEntity booking = hotelBookingRepository.findDetailById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đặt phòng"));
        if (!booking.getUser().getId().equals(userId) && !SecurityUtil.hasRole("ADMIN")) {
            throw ApiException.notFound("Không tìm thấy đặt phòng");
        }
        return booking;
    }

    private HotelBookingDTO toDTO(HotelBookingEntity booking) {
        return hotelBookingDTOConverter.toHotelBookingDTO(booking, findLatestPayment(booking.getOrder()),
                isCancellable(booking, LocalDateTime.now()));
    }

    private boolean isCancellable(HotelBookingEntity booking, LocalDateTime now) {
        return "pending".equals(booking.getStatus())
                || ("confirmed".equals(booking.getStatus()) && isFreeCancellation(booking, now));
    }

    private PaymentEntity findLatestPayment(OrderEntity order) {
        List<PaymentEntity> payments = paymentRepository.findByOrderIds(List.of(order.getId()));
        return payments.isEmpty() ? null : payments.get(0);
    }

    private RoomTypeEntity findActiveRoomType(UUID hotelId, UUID roomTypeId) {
        RoomTypeEntity roomType = roomTypeRepository.findByIdAndHotelId(roomTypeId, hotelId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy loại phòng của khách sạn"));
        if (!Boolean.TRUE.equals(roomType.getIsActive())) {
            throw ApiException.badRequest("Loại phòng này hiện không nhận đặt");
        }
        return roomType;
    }

    private int validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw ApiException.badRequest("Vui lòng chọn ngày nhận và trả phòng");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw ApiException.badRequest("Ngày nhận phòng không được ở quá khứ");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw ApiException.badRequest("Ngày trả phòng phải sau ngày nhận phòng");
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights > MAX_NIGHTS) {
            throw ApiException.badRequest("Chỉ được đặt tối đa " + MAX_NIGHTS + " đêm");
        }
        return (int) nights;
    }

    private BigDecimal priceOf(RoomTypeEntity roomType) {
        return roomType.getPricePerNight() != null ? BigDecimal.valueOf(roomType.getPricePerNight()) : BigDecimal.ZERO;
    }

    private String generateOrderCode() {
        String prefix = "TN" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmm"));
        String code;
        do {
            code = prefix + String.format("%04d", RANDOM.nextInt(10_000));
        } while (orderRepository.existsByOrderCode(code));
        return code;
    }
}
