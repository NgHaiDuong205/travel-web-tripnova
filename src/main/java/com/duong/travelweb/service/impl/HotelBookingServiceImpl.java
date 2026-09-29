package com.duong.travelweb.service.impl;

import com.duong.travelweb.converter.HotelBookingDTOConverter;
import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.CartCheckoutDTO;
import com.duong.travelweb.model.dto.HotelBookingCreatedDTO;
import com.duong.travelweb.model.dto.HotelBookingDTO;
import com.duong.travelweb.model.dto.HotelBookingRequestDTO;
import com.duong.travelweb.model.dto.InvoiceItemDTO;
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
import com.duong.travelweb.service.NotificationService;
import com.duong.travelweb.service.HotelBookingService;
import com.duong.travelweb.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private static final List<String> ADMIN_REFUNDABLE_STATUSES = List.of("confirmed", "no_show", "checked_out", "completed");

    private final NotificationService notificationService;
    private final HotelBookingRepository hotelBookingRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomAvailabilityRepository roomAvailabilityRepository;
    private final UserRepository userRepository;
    private final HotelBookingDTOConverter hotelBookingDTOConverter;
    private final OrderFactory orderFactory;
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
                                   OrderFactory orderFactory,
                                   NotificationService notificationService,
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
        this.orderFactory = orderFactory;
        this.notificationService = notificationService;
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

        PlannedBooking planned = new PlannedBooking(roomType, room, request.getCheckIn(), request.getCheckOut(), nights,
                request.getAdults(), children, request.getSpecialRequests());
        CreatedOrder created = persistOrder(userId, List.of(planned), request.getPaymentMethod());

        HotelBookingCreatedDTO dto = new HotelBookingCreatedDTO();
        dto.setOrderId(created.order().getId());
        dto.setOrderCode(created.order().getOrderCode());
        dto.setBookingId(created.bookings().get(0).getId());
        dto.setPaymentId(created.payment().getId());
        dto.setAmount(created.payment().getAmount());
        dto.setCurrencyCode(currencyCode);
        dto.setPaymentUrl(frontendUrl + "/payment/" + created.payment().getId());
        dto.setHoldExpiresAt(created.order().getCreatedAt().plusMinutes(holdMinutes));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public int countBookableRooms(UUID roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        return findBookableRooms(roomTypeId, checkIn, checkOut, null).size();
    }

    @Override
    @Transactional
    public CartCheckoutDTO createOrder(UUID userId, List<HotelBookingLine> lines, String paymentMethod) {
        if (lines.isEmpty()) {
            throw ApiException.badRequest("Không có phòng nào để đặt");
        }
        // Phòng đã chọn trong lần đặt này (roomId -> các khoảng [checkIn, checkOut)) để 2 dòng không lấy trùng phòng.
        Map<UUID, List<LocalDate[]>> taken = new HashMap<>();
        List<PlannedBooking> planned = new ArrayList<>();
        for (HotelBookingLine line : lines) {
            int nights = validateDates(line.checkIn(), line.checkOut());
            RoomTypeEntity roomType = findActiveRoomType(line.hotelId(), line.roomTypeId());
            HotelEntity hotel = roomType.getHotel();
            if (!Boolean.TRUE.equals(hotel.getIsActive())) {
                throw ApiException.badRequest("Khách sạn " + hotel.getName() + " hiện không nhận đặt phòng");
            }
            if (line.adults() < line.quantity()) {
                throw ApiException.badRequest("Mỗi phòng cần ít nhất 1 người lớn (" + roomType.getName() + ")");
            }
            int maxPerRoom = ceilDiv(line.adults(), line.quantity()) + ceilDiv(line.children(), line.quantity());
            if (roomType.getMaxOccupancy() != null && maxPerRoom > roomType.getMaxOccupancy()) {
                throw ApiException.badRequest("Số khách mỗi phòng vượt quá sức chứa " + roomType.getMaxOccupancy()
                        + " người của " + roomType.getName() + ", hãy đặt thêm phòng");
            }
            List<RoomEntity> free = findBookableRooms(roomType.getId(), line.checkIn(), line.checkOut(), null).stream()
                    .filter(r -> !overlapsTaken(taken.get(r.getId()), line.checkIn(), line.checkOut()))
                    .toList();
            if (free.size() < line.quantity()) {
                throw ApiException.conflict(roomType.getName() + " - " + hotel.getName() + " chỉ còn " + free.size()
                        + " phòng trống từ " + line.checkIn() + " đến " + line.checkOut());
            }
            for (int i = 0; i < line.quantity(); i++) {
                RoomEntity room = free.get(i);
                taken.computeIfAbsent(room.getId(), k -> new ArrayList<>()).add(new LocalDate[]{line.checkIn(), line.checkOut()});
                // Chia đều khách: các phòng đầu nhận phần dư
                int adults = line.adults() / line.quantity() + (i < line.adults() % line.quantity() ? 1 : 0);
                int children = line.children() / line.quantity() + (i < line.children() % line.quantity() ? 1 : 0);
                planned.add(new PlannedBooking(roomType, room, line.checkIn(), line.checkOut(), nights,
                        adults, children, line.specialRequests()));
            }
        }
        CreatedOrder created = persistOrder(userId, planned, paymentMethod);

        CartCheckoutDTO dto = new CartCheckoutDTO();
        dto.setOrderId(created.order().getId());
        dto.setOrderCode(created.order().getOrderCode());
        dto.setBookingIds(new ArrayList<>(created.bookings().stream().map(HotelBookingEntity::getId).toList()));
        dto.setPaymentId(created.payment().getId());
        dto.setAmount(created.payment().getAmount());
        dto.setCurrencyCode(currencyCode);
        dto.setPaymentUrl(frontendUrl + "/payment/" + created.payment().getId());
        dto.setHoldExpiresAt(created.order().getCreatedAt().plusMinutes(holdMinutes));
        return dto;
    }

    /** Booking đã chọn được phòng, chờ ghi DB. */
    private record PlannedBooking(RoomTypeEntity roomType, RoomEntity room, LocalDate checkIn, LocalDate checkOut,
                                  int nights, int adults, int children, String specialRequests) {
    }

    private record CreatedOrder(OrderEntity order, List<HotelBookingEntity> bookings, PaymentEntity payment) {
    }

    /** Ghi order (pending) + các booking (pending, giữ phòng theo hold-minutes) + payment (pending) cho tổng tiền. */
    private CreatedOrder persistOrder(UUID userId, List<PlannedBooking> planned, String paymentMethod) {
        UserEntity user = userRepository.getReferenceById(userId);
        LocalDateTime now = LocalDateTime.now();
        BigDecimal total = planned.stream()
                .map(p -> priceOf(p.roomType()).multiply(BigDecimal.valueOf(p.nights())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OrderFactory.PendingOrder pending = orderFactory.createPendingOrder(userId, total, paymentMethod);
        OrderEntity order = pending.order();

        List<HotelBookingEntity> bookings = new ArrayList<>();
        for (PlannedBooking p : planned) {
            HotelBookingEntity booking = new HotelBookingEntity();
            booking.setOrder(order);
            booking.setUser(user);
            booking.setHotel(p.roomType().getHotel());
            booking.setRoomType(p.roomType());
            booking.setRoom(p.room());
            booking.setCheckInDate(p.checkIn());
            booking.setCheckOutDate(p.checkOut());
            booking.setNumNights(p.nights());
            booking.setNumAdults(p.adults());
            booking.setNumChildren(p.children());
            booking.setTotalPrice(priceOf(p.roomType()).multiply(BigDecimal.valueOf(p.nights())));
            booking.setDiscountAmount(BigDecimal.ZERO);
            booking.setSpecialRequests(p.specialRequests() != null && !p.specialRequests().isBlank()
                    ? p.specialRequests().trim() : null);
            booking.setStatus("pending");
            booking.setCreatedAt(now);
            booking.setUpdatedAt(now);
            bookings.add(hotelBookingRepository.save(booking));
        }

        return new CreatedOrder(order, bookings, pending.payment());
    }

    private boolean overlapsTaken(List<LocalDate[]> ranges, LocalDate checkIn, LocalDate checkOut) {
        return ranges != null && ranges.stream().anyMatch(r -> r[0].isBefore(checkOut) && r[1].isAfter(checkIn));
    }

    private int ceilDiv(int a, int b) {
        return (a + b - 1) / b;
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
        // Order nhiều booking: chỉ hoàn đủ khi mọi booking đã hoàn (payment -> refunded trong sync)
        syncOrderRefundStatus(order, now);
        if ("refunded".equals(order.getStatus())) {
            order.setCancelledAt(now);
            order.setCancelReason(reason);
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
        notificationService.notifyBookingStatus(booking.getUser().getId(), "hotel", booking.getId(), booking.getStatus(), reason);
        return toDTO(booking);
    }

    @Override
    @Transactional
    public HotelBookingDTO refundByAdmin(UUID bookingId, BigDecimal amount, String reason) {
        HotelBookingEntity booking = hotelBookingRepository.findDetailById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đặt phòng"));
        OrderEntity order = booking.getOrder();
        requirePaidOrder(order);
        if (!ADMIN_REFUNDABLE_STATUSES.contains(booking.getStatus())) {
            throw ApiException.badRequest("Không thể hoàn tiền đặt phòng ở trạng thái " + booking.getStatus());
        }
        LocalDateTime now = LocalDateTime.now();
        applyAdminRefund(booking, amount, adminRefundNote(reason), now);
        syncOrderRefundStatus(order, now);
        notificationService.notify(booking.getUser().getId(), "refund", "A refund was issued for your hotel booking",
                reason != null && !reason.isBlank() ? reason.trim() : null, "/bookings/" + booking.getId(), "hotel_booking",
                booking.getId());
        return toDTO(booking);
    }

    @Override
    @Transactional
    public void refundOrderByAdmin(OrderEntity order, String reason) {
        requirePaidOrder(order);
        LocalDateTime now = LocalDateTime.now();
        String note = adminRefundNote(reason);
        boolean refunded = false;
        for (HotelBookingEntity booking : hotelBookingRepository.findByOrderId(order.getId())) {
            if (ADMIN_REFUNDABLE_STATUSES.contains(booking.getStatus()) && remainingRefundable(booking).signum() > 0) {
                applyAdminRefund(booking, null, note, now);
                refunded = true;
            }
        }
        if (!refunded) {
            throw ApiException.badRequest("Đơn hàng không còn đặt phòng nào có thể hoàn tiền");
        }
        syncOrderRefundStatus(order, now);
    }

    @Override
    @Transactional
    public void deleteByAdmin(UUID bookingId) {
        HotelBookingEntity booking = hotelBookingRepository.findDetailById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đặt phòng"));
        if (!"cancelled".equals(booking.getStatus())) {
            throw ApiException.conflict("Chỉ được xoá đặt phòng đã huỷ");
        }
        OrderEntity order = booking.getOrder();
        List<PaymentEntity> payments = paymentRepository.findByOrderIds(List.of(order.getId()));
        boolean everPaid = payments.stream().anyMatch(p -> "success".equals(p.getStatus()) || "refunded".equals(p.getStatus()));
        if (everPaid || booking.getRefundedAt() != null) {
            throw ApiException.conflict("Đặt phòng đã phát sinh thanh toán, không thể xoá (cần giữ lịch sử giao dịch)");
        }
        hotelBookingRepository.delete(booking);
        hotelBookingRepository.flush();
        if (hotelBookingRepository.findByOrderId(order.getId()).isEmpty()) {
            if (orderRepository.countInvoices(order.getId()) > 0) {
                return; // đơn đã có hoá đơn -> giữ lại order/payment
            }
            paymentRepository.deleteAll(payments);
            orderRepository.delete(order);
        }
    }

    /**
     * Hoàn thêm tiền cho booking (cộng dồn vào refund_amount). Hoàn đủ -> status refunded và trả phòng nếu chưa ở;
     * hoàn một phần (bồi thường) -> giữ nguyên trạng thái.
     */
    private void applyAdminRefund(HotelBookingEntity booking, BigDecimal amount, String reason, LocalDateTime now) {
        BigDecimal remaining = remainingRefundable(booking);
        if (remaining.signum() <= 0) {
            throw ApiException.badRequest("Đặt phòng đã được hoàn đủ tiền");
        }
        BigDecimal refund = amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : remaining;
        if (refund.signum() <= 0) {
            throw ApiException.badRequest("Số tiền hoàn phải lớn hơn 0");
        }
        if (refund.compareTo(remaining) > 0) {
            throw ApiException.badRequest("Số tiền hoàn vượt quá số còn có thể hoàn (" + remaining + ")");
        }
        BigDecimal already = booking.getRefundAmount() != null ? booking.getRefundAmount() : BigDecimal.ZERO;
        booking.setRefundAmount(already.add(refund));
        booking.setRefundReason(reason);
        booking.setRefundedAt(now);
        booking.setUpdatedAt(now);
        if (refund.compareTo(remaining) == 0) {
            if (booking.getRoom() != null && ("confirmed".equals(booking.getStatus()) || "no_show".equals(booking.getStatus()))) {
                roomAvailabilityRepository.releaseDays(booking.getRoom().getId(),
                        booking.getCheckInDate(), booking.getCheckOutDate());
            }
            booking.setStatus("refunded");
        }
    }

    private BigDecimal remainingRefundable(HotelBookingEntity booking) {
        BigDecimal already = booking.getRefundAmount() != null ? booking.getRefundAmount() : BigDecimal.ZERO;
        return booking.getTotalPrice().subtract(already);
    }

    /** Order hoàn đủ -> refunded (payment cũng refunded); hoàn một phần -> partially_refunded. */
    private void syncOrderRefundStatus(OrderEntity order, LocalDateTime now) {
        BigDecimal refunded = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (HotelBookingEntity booking : hotelBookingRepository.findByOrderId(order.getId())) {
            total = total.add(booking.getTotalPrice());
            if (booking.getRefundAmount() != null) {
                refunded = refunded.add(booking.getRefundAmount());
            }
        }
        boolean full = refunded.compareTo(total) >= 0;
        order.setStatus(full ? "refunded" : "partially_refunded");
        order.setUpdatedAt(now);
        if (full) {
            PaymentEntity payment = orderFactory.findLatestPayment(order);
            if (payment != null && "success".equals(payment.getStatus())) {
                payment.setStatus("refunded");
            }
        }
    }

    private void requirePaidOrder(OrderEntity order) {
        if (!"paid".equals(order.getStatus()) && !"partially_refunded".equals(order.getStatus())) {
            throw ApiException.badRequest("Đơn hàng chưa thanh toán hoặc đã hoàn tiền, không thể hoàn tiền");
        }
    }

    private String adminRefundNote(String reason) {
        return reason != null && !reason.isBlank() ? reason.trim() : "Quản trị viên hoàn tiền";
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
    public String bookingType() {
        return "hotel";
    }

    @Override
    @Transactional(readOnly = true)
    public boolean handles(UUID orderId) {
        return hotelBookingRepository.existsByOrderId(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> bookingIds(UUID orderId) {
        return sortedBookings(orderId).stream().map(HotelBookingEntity::getId).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceItemDTO> invoiceItems(UUID orderId) {
        List<InvoiceItemDTO> items = new ArrayList<>();
        for (HotelBookingEntity booking : sortedBookings(orderId)) {
            InvoiceItemDTO item = new InvoiceItemDTO();
            item.setItemType("hotel");
            item.setBookingId(booking.getId());
            item.setTitle(booking.getHotel().getName());
            item.setSubtitle(booking.getRoomType().getName());
            item.setHotelName(booking.getHotel().getName());
            item.setRoomTypeName(booking.getRoomType().getName());
            item.setCheckInDate(booking.getCheckInDate());
            item.setCheckOutDate(booking.getCheckOutDate());
            item.setNights(booking.getNumNights());
            item.setGuests((booking.getNumAdults() == null ? 0 : booking.getNumAdults())
                    + (booking.getNumChildren() == null ? 0 : booking.getNumChildren()));
            item.setAmount(booking.getTotalPrice());
            item.setRefundAmount(booking.getRefundAmount() == null ? BigDecimal.ZERO : booking.getRefundAmount());
            item.setStatus(booking.getStatus());
            items.add(item);
        }
        return items;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, BigDecimal> refundedAmounts(Collection<UUID> orderIds) {
        Map<UUID, BigDecimal> refunds = new HashMap<>();
        if (!orderIds.isEmpty()) {
            for (Object[] row : hotelBookingRepository.sumRefundByOrderIds(new ArrayList<>(orderIds))) {
                refunds.put((UUID) row[0], (BigDecimal) row[1]);
            }
        }
        return refunds;
    }

    private List<HotelBookingEntity> sortedBookings(UUID orderId) {
        return hotelBookingRepository.findByOrderId(orderId).stream()
                .sorted(Comparator.comparing(HotelBookingEntity::getCheckInDate)
                        .thenComparing(b -> b.getId().toString()))
                .toList();
    }

    @Override
    @Transactional
    public boolean confirmOrder(OrderEntity order) {
        LocalDateTime now = LocalDateTime.now();
        List<HotelBookingEntity> bookings = hotelBookingRepository.findByOrderId(order.getId());
        boolean allConfirmed = true;
        boolean anyConfirmed = false;

        for (HotelBookingEntity booking : bookings) {
            if ("confirmed".equals(booking.getStatus())) {
                anyConfirmed = true;
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
            anyConfirmed = true;
        }

        // Order nhiều booking: phòng nào mất thì chỉ hoàn booking đó (partially_refunded), payment vẫn success.
        order.setStatus(allConfirmed ? "paid" : anyConfirmed ? "partially_refunded" : "refunded");
        if (!anyConfirmed) {
            order.setCancelledAt(now);
            order.setCancelReason("Hết phòng, đã hoàn tiền");
        } else {
            order.setCancelledAt(null);
            order.setCancelReason(null);
        }
        order.setUpdatedAt(now);
        return anyConfirmed;
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
        PaymentEntity payment = orderFactory.findLatestPayment(order);
        if (payment != null && "pending".equals(payment.getStatus())) {
            payment.setStatus("failed");
        }
    }

    @Override
    @Transactional
    public int expirePendingBookings() {
        List<HotelBookingEntity> expired = hotelBookingRepository.findPendingCreatedBefore(
                LocalDateTime.now().minusMinutes(holdMinutes));
        // Order có thể gồm nhiều booking (đặt từ giỏ) -> huỷ + báo 1 lần mỗi order.
        Map<UUID, OrderEntity> orders = new LinkedHashMap<>();
        for (HotelBookingEntity booking : expired) {
            orders.putIfAbsent(booking.getOrder().getId(), booking.getOrder());
        }
        orders.values().forEach(order -> cancelPendingOrder(order, EXPIRED_REASON));
        orders.values().forEach(order -> notificationService.notifyOrder(order, "booking_expired",
                "Booking hold expired - order " + order.getOrderCode(),
                "The order was not paid in time, so the reservation was released. You can book again at any time."));
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
        return hotelBookingDTOConverter.toHotelBookingDTO(booking, orderFactory.findLatestPayment(booking.getOrder()),
                isCancellable(booking, LocalDateTime.now()));
    }

    private boolean isCancellable(HotelBookingEntity booking, LocalDateTime now) {
        return "pending".equals(booking.getStatus())
                || ("confirmed".equals(booking.getStatus()) && isFreeCancellation(booking, now));
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
}
