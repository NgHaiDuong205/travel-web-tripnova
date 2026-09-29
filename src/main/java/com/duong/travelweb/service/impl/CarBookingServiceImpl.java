package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.CarAvailabilityDTO;
import com.duong.travelweb.model.dto.CarBookingDTO;
import com.duong.travelweb.model.dto.CarBookingRequestDTO;
import com.duong.travelweb.model.dto.CarBusyRangeDTO;
import com.duong.travelweb.model.dto.InvoiceItemDTO;
import com.duong.travelweb.model.entity.CarBookingEntity;
import com.duong.travelweb.model.entity.CarEntity;
import com.duong.travelweb.model.entity.OrderEntity;
import com.duong.travelweb.model.entity.PaymentEntity;
import com.duong.travelweb.model.entity.UserEntity;
import com.duong.travelweb.repository.CarBookingRepository;
import com.duong.travelweb.repository.CarRepository;
import com.duong.travelweb.repository.PaymentRepository;
import com.duong.travelweb.repository.UserRepository;
import com.duong.travelweb.service.NotificationService;
import com.duong.travelweb.service.CarBookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CarBookingServiceImpl implements CarBookingService {
    private static final Logger log = LoggerFactory.getLogger(CarBookingServiceImpl.class);
    private static final String EXPIRED_REASON = "Hết thời gian giữ xe, đơn chưa được thanh toán";
    private static final String TAKEN_REASON = "Xe đã được khách khác đặt trước khi thanh toán hoàn tất";
    /** Đặt xe phải trước giờ nhận xe ít nhất chừng này. */
    private static final Duration MIN_LEAD_TIME = Duration.ofHours(1);
    private static final List<String> ADMIN_REFUNDABLE = List.of("confirmed", "no_show", "checked_out", "completed");

    private final NotificationService notificationService;
    private final CarBookingRepository carBookingRepository;
    private final CarRepository carRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final OrderFactory orderFactory;

    public CarBookingServiceImpl(CarBookingRepository carBookingRepository,
                                 CarRepository carRepository,
                                 PaymentRepository paymentRepository,
                                 UserRepository userRepository,
                                 OrderFactory orderFactory,
                                 NotificationService notificationService) {
        this.carBookingRepository = carBookingRepository;
        this.carRepository = carRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.orderFactory = orderFactory;
        this.notificationService = notificationService;
    }

    // ================= Public / khách =================

    @Override
    @Transactional(readOnly = true)
    public CarAvailabilityDTO availability(UUID carId, LocalDateTime from, LocalDateTime to) {
        CarEntity car = carRepository.findById(carId)
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy xe"));
        if (from == null || to == null || !to.isAfter(from)) {
            throw ApiException.badRequest("Thời gian trả xe phải sau thời gian nhận xe");
        }
        List<CarBookingEntity> blocking = carBookingRepository.findBlocking(carId, null, from, to, orderFactory.holdCutoff());
        CarAvailabilityDTO dto = new CarAvailabilityDTO();
        dto.setCarId(carId);
        dto.setFrom(from);
        dto.setTo(to);
        dto.setAvailable(blocking.isEmpty());
        int days = rentalDays(from, to);
        dto.setRentalDays(days);
        dto.setTotalPrice(car.getPricePerDay().multiply(BigDecimal.valueOf(days)));
        dto.setCurrencyCode(orderFactory.currencyCode());
        List<CarBusyRangeDTO> ranges = new ArrayList<>();
        for (CarBookingEntity booking : blocking) {
            CarBusyRangeDTO range = new CarBusyRangeDTO();
            range.setFrom(booking.getPickupDate());
            range.setTo(booking.getReturnDate());
            ranges.add(range);
        }
        dto.setBusyRanges(ranges);
        return dto;
    }

    @Override
    @Transactional
    public CarBookingDTO create(UUID userId, CarBookingRequestDTO request) {
        LocalDateTime pickup = request.getPickupDate();
        LocalDateTime dropOff = request.getReturnDate();
        validatePeriod(pickup, dropOff);
        // Khoá xe: hai người đặt cùng lúc sẽ xếp hàng ở đây, người sau thấy booking pending của người trước.
        CarEntity car = carRepository.lockById(request.getCarId())
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy xe"));
        String license = blankToNull(request.getDriverLicenseNo());
        if (!Boolean.TRUE.equals(car.getWithDriver()) && license == null) {
            throw ApiException.badRequest("Xe tự lái: vui lòng nhập số giấy phép lái xe");
        }
        if (!carBookingRepository.findBlocking(car.getId(), null, pickup, dropOff, orderFactory.holdCutoff()).isEmpty()) {
            throw ApiException.conflict("Xe đã có người đặt trong khoảng thời gian này, hãy chọn thời gian khác");
        }

        BigDecimal total = car.getPricePerDay().multiply(BigDecimal.valueOf(rentalDays(pickup, dropOff)));
        OrderFactory.PendingOrder pending = orderFactory.createPendingOrder(userId, total, request.getPaymentMethod());
        LocalDateTime now = LocalDateTime.now();
        String pickupLocation = blankToNull(request.getPickupLocation()) != null
                ? request.getPickupLocation().trim() : car.getPickupLocation();
        String returnLocation = blankToNull(request.getReturnLocation()) != null
                ? request.getReturnLocation().trim() : pickupLocation;

        CarBookingEntity booking = new CarBookingEntity();
        booking.setOrder(pending.order());
        booking.setUser(userRepository.getReferenceById(userId));
        booking.setCar(car);
        booking.setPickupDate(pickup);
        booking.setReturnDate(dropOff);
        booking.setPickupLocation(pickupLocation);
        booking.setReturnLocation(returnLocation);
        booking.setDriverLicenseNo(license);
        booking.setTotalPrice(total);
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setSpecialRequests(blankToNull(request.getSpecialRequests()));
        booking.setStatus("pending");
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);
        booking = carBookingRepository.save(booking);
        return toDTO(booking, pending.payment());
    }

    @Override
    @Transactional(readOnly = true)
    public CarBookingDTO get(UUID userId, boolean isAdmin, UUID bookingId) {
        CarBookingEntity booking = carBookingRepository.findDetailById(bookingId)
                .filter(b -> isAdmin || b.getUser().getId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn thuê xe"));
        return toDTO(booking, orderFactory.findLatestPayment(booking.getOrder()));
    }

    @Override
    @Transactional
    public CarBookingDTO cancel(UUID userId, UUID bookingId, String reason) {
        CarBookingEntity booking = carBookingRepository.findDetailById(bookingId)
                .filter(b -> b.getUser().getId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn thuê xe"));
        String note = reason != null && !reason.isBlank() ? reason.trim() : "Khách hàng huỷ thuê xe";
        switch (booking.getStatus()) {
            case "pending" -> cancelPendingOrder(booking.getOrder(), note);
            case "confirmed" -> {
                if (!isFreeCancellation(booking, LocalDateTime.now())) {
                    throw ApiException.badRequest("Đã quá hạn huỷ miễn phí (" + FREE_CANCELLATION_HOURS
                            + " giờ trước giờ nhận xe), không thể huỷ");
                }
                refundBooking(booking, note, LocalDateTime.now());
            }
            default -> throw ApiException.badRequest("Không thể huỷ đơn thuê xe ở trạng thái " + booking.getStatus());
        }
        return toDTO(booking, orderFactory.findLatestPayment(booking.getOrder()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarBookingDTO> findMine(UUID userId, String statusGroup, int page, int limit) {
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, limit);
        List<String> statuses = statusesOf(statusGroup);
        Page<CarBookingEntity> bookings = statuses == null
                ? carBookingRepository.findByUser(userId, pageable)
                : carBookingRepository.findByUserAndStatuses(userId, statuses, pageable);
        return new PageImpl<>(toDTOs(bookings.getContent()), pageable, bookings.getTotalElements());
    }

    // ================= Admin =================

    @Override
    @Transactional(readOnly = true)
    public Page<CarBookingDTO> findForAdmin(String status, String keyword, int page, int limit) {
        List<CarBookingEntity> bookings = carBookingRepository.findForAdmin(status, keyword, page, limit);
        long total = carBookingRepository.countForAdmin(status, keyword);
        return new PageImpl<>(toDTOs(bookings), PageRequest.of(Math.max(page, 1) - 1, limit), total);
    }

    @Override
    @Transactional
    public CarBookingDTO updateStatusByAdmin(UUID bookingId, String status, String reason) {
        CarBookingEntity booking = carBookingRepository.findDetailById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn thuê xe"));
        String next = status == null ? "" : status.trim().toLowerCase();
        String note = reason != null && !reason.isBlank() ? reason.trim() : "Quản trị viên huỷ đơn thuê xe";
        LocalDateTime now = LocalDateTime.now();
        switch (booking.getStatus() + "->" + next) {
            case "pending->cancelled" -> cancelPendingOrder(booking.getOrder(), note);
            case "confirmed->cancelled" -> refundBooking(booking, note, now);
            case "confirmed->checked_in", "checked_in->checked_out", "checked_out->completed", "confirmed->no_show" -> {
                booking.setStatus(next);
                booking.setUpdatedAt(now);
            }
            default -> throw ApiException.badRequest("Không thể chuyển trạng thái từ " + booking.getStatus() + " sang " + next);
        }
        notificationService.notifyBookingStatus(booking.getUser().getId(), "car", booking.getId(), booking.getStatus(), reason);
        return toDTO(booking, orderFactory.findLatestPayment(booking.getOrder()));
    }

    // ================= OrderBookingHandler =================

    @Override
    public String bookingType() {
        return "car";
    }

    @Override
    @Transactional(readOnly = true)
    public boolean handles(UUID orderId) {
        return carBookingRepository.existsByOrderId(orderId);
    }

    @Override
    @Transactional
    public boolean confirmOrder(OrderEntity order) {
        LocalDateTime now = LocalDateTime.now();
        boolean anyConfirmed = false;
        for (CarBookingEntity booking : carBookingRepository.findByOrderId(order.getId())) {
            if ("confirmed".equals(booking.getStatus())) {
                anyConfirmed = true;
                continue;
            }
            // pending, hoặc đã bị job huỷ vì hết hạn nhưng tiền về muộn -> thử giữ xe lại
            CarEntity car = carRepository.lockById(booking.getCar().getId()).orElseThrow();
            boolean taken = !carBookingRepository.findBlocking(car.getId(), booking.getId(), booking.getPickupDate(),
                    booking.getReturnDate(), orderFactory.holdCutoff()).isEmpty();
            if (taken || !Boolean.TRUE.equals(car.getIsActive())) {
                booking.setStatus("refunded");
            } else {
                booking.setStatus("confirmed");
                anyConfirmed = true;
            }
            booking.setUpdatedAt(now);
        }
        order.setStatus(anyConfirmed ? "paid" : "refunded");
        order.setCancelledAt(anyConfirmed ? null : now);
        order.setCancelReason(anyConfirmed ? null : TAKEN_REASON);
        order.setUpdatedAt(now);
        return anyConfirmed;
    }

    @Override
    @Transactional
    public void cancelPendingOrder(OrderEntity order, String reason) {
        LocalDateTime now = LocalDateTime.now();
        for (CarBookingEntity booking : carBookingRepository.findByOrderId(order.getId())) {
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
    public void refundOrderByAdmin(OrderEntity order, String reason) {
        if (!"paid".equals(order.getStatus()) && !"partially_refunded".equals(order.getStatus())) {
            throw ApiException.badRequest("Đơn hàng chưa thanh toán hoặc đã hoàn tiền, không thể hoàn tiền");
        }
        String note = reason != null && !reason.isBlank() ? reason.trim() : "Quản trị viên hoàn tiền";
        boolean refunded = false;
        for (CarBookingEntity booking : carBookingRepository.findByOrderId(order.getId())) {
            if (ADMIN_REFUNDABLE.contains(booking.getStatus())) {
                refundBooking(booking, note, LocalDateTime.now());
                refunded = true;
            }
        }
        if (!refunded) {
            throw ApiException.badRequest("Đơn hàng không còn đơn thuê xe nào có thể hoàn tiền");
        }
    }

    @Override
    @Transactional
    public int expirePendingBookings() {
        List<CarBookingEntity> expired = carBookingRepository.findPendingCreatedBefore(orderFactory.holdCutoff());
        for (CarBookingEntity booking : expired) {
            cancelPendingOrder(booking.getOrder(), EXPIRED_REASON);
        }
        if (!expired.isEmpty()) {
            log.info("Expired {} pending car bookings", expired.size());
        }
        return expired.size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> bookingIds(UUID orderId) {
        return carBookingRepository.findByOrderId(orderId).stream().map(CarBookingEntity::getId).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceItemDTO> invoiceItems(UUID orderId) {
        List<InvoiceItemDTO> items = new ArrayList<>();
        for (CarBookingEntity booking : carBookingRepository.findByOrderId(orderId)) {
            CarEntity car = booking.getCar();
            InvoiceItemDTO item = new InvoiceItemDTO();
            item.setItemType("car");
            item.setBookingId(booking.getId());
            item.setTitle(car.getName());
            item.setSubtitle(joinNonBlank(" ", car.getBrand(), car.getModel())
                    + (Boolean.TRUE.equals(car.getWithDriver()) ? " · with driver" : " · self-drive"));
            item.setCheckInDate(booking.getPickupDate().toLocalDate());
            item.setCheckOutDate(booking.getReturnDate().toLocalDate());
            item.setNights(rentalDays(booking.getPickupDate(), booking.getReturnDate()));
            item.setAmount(booking.getTotalPrice());
            item.setRefundAmount("refunded".equals(booking.getStatus()) ? booking.getTotalPrice() : BigDecimal.ZERO);
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
            for (Object[] row : carBookingRepository.sumRefundByOrderIds(orderIds)) {
                refunds.put((UUID) row[0], (BigDecimal) row[1]);
            }
        }
        return refunds;
    }

    // ================= helpers =================

    /** Hoàn 100% booking đã thanh toán; lý do lưu ở orders.cancel_reason (car_bookings không có cột riêng). */
    private void refundBooking(CarBookingEntity booking, String reason, LocalDateTime now) {
        booking.setStatus("refunded");
        booking.setUpdatedAt(now);
        OrderEntity order = booking.getOrder();
        boolean allRefunded = carBookingRepository.findByOrderId(order.getId()).stream()
                .allMatch(b -> b.getId().equals(booking.getId()) || "refunded".equals(b.getStatus())
                        || "cancelled".equals(b.getStatus()));
        order.setStatus(allRefunded ? "refunded" : "partially_refunded");
        order.setCancelReason(reason);
        if (allRefunded) {
            order.setCancelledAt(now);
            PaymentEntity payment = orderFactory.findLatestPayment(order);
            if (payment != null && "success".equals(payment.getStatus())) {
                payment.setStatus("refunded");
            }
        }
        order.setUpdatedAt(now);
    }

    private void validatePeriod(LocalDateTime pickup, LocalDateTime dropOff) {
        if (pickup == null || dropOff == null) {
            throw ApiException.badRequest("Vui lòng chọn thời gian nhận và trả xe");
        }
        if (pickup.isBefore(LocalDateTime.now().plus(MIN_LEAD_TIME))) {
            throw ApiException.badRequest("Giờ nhận xe phải sau thời điểm hiện tại ít nhất 1 giờ");
        }
        if (!dropOff.isAfter(pickup)) {
            throw ApiException.badRequest("Thời gian trả xe phải sau thời gian nhận xe");
        }
        if (rentalDays(pickup, dropOff) > MAX_RENTAL_DAYS) {
            throw ApiException.badRequest("Chỉ được thuê tối đa " + MAX_RENTAL_DAYS + " ngày");
        }
    }

    /** Số ngày tính tiền: làm tròn lên theo từng 24 giờ, tối thiểu 1. */
    static int rentalDays(LocalDateTime pickup, LocalDateTime dropOff) {
        long minutes = Duration.between(pickup, dropOff).toMinutes();
        return (int) Math.max(1, (minutes + 1439) / 1440);
    }

    private boolean isFreeCancellation(CarBookingEntity booking, LocalDateTime now) {
        return now.isBefore(booking.getPickupDate().minusHours(FREE_CANCELLATION_HOURS));
    }

    private List<String> statusesOf(String statusGroup) {
        if (statusGroup == null || statusGroup.isBlank() || "all".equals(statusGroup)) {
            return null;
        }
        return switch (statusGroup) {
            case "upcoming" -> List.of("confirmed", "checked_in");
            case "pending" -> List.of("pending");
            case "completed" -> List.of("checked_out", "completed", "no_show");
            case "cancelled" -> List.of("cancelled", "refunded");
            default -> throw ApiException.badRequest("Trạng thái lọc không hợp lệ: " + statusGroup);
        };
    }

    private List<CarBookingDTO> toDTOs(List<CarBookingEntity> bookings) {
        if (bookings.isEmpty()) {
            return List.of();
        }
        List<UUID> orderIds = bookings.stream().map(b -> b.getOrder().getId()).distinct().toList();
        Map<UUID, PaymentEntity> latest = new HashMap<>();
        for (PaymentEntity payment : paymentRepository.findByOrderIds(orderIds)) {
            latest.putIfAbsent(payment.getOrder().getId(), payment);
        }
        return bookings.stream().map(b -> toDTO(b, latest.get(b.getOrder().getId()))).toList();
    }

    private CarBookingDTO toDTO(CarBookingEntity booking, PaymentEntity payment) {
        CarEntity car = booking.getCar();
        OrderEntity order = booking.getOrder();
        LocalDateTime now = LocalDateTime.now();
        CarBookingDTO dto = new CarBookingDTO();
        dto.setId(booking.getId());
        dto.setOrderId(order.getId());
        dto.setOrderCode(order.getOrderCode());
        dto.setOrderStatus(order.getStatus());
        dto.setCarId(car.getId());
        dto.setCarName(car.getName());
        dto.setCarBrand(car.getBrand());
        dto.setCarModel(car.getModel());
        dto.setCarType(car.getCarType());
        dto.setCarImageUrl(car.getCoverImageUrl());
        dto.setWithDriver(Boolean.TRUE.equals(car.getWithDriver()));
        dto.setPickupDate(booking.getPickupDate());
        dto.setReturnDate(booking.getReturnDate());
        dto.setRentalDays(rentalDays(booking.getPickupDate(), booking.getReturnDate()));
        dto.setPickupLocation(booking.getPickupLocation());
        dto.setReturnLocation(booking.getReturnLocation());
        dto.setDriverLicenseNo(booking.getDriverLicenseNo());
        dto.setPricePerDay(car.getPricePerDay());
        dto.setTotalPrice(booking.getTotalPrice());
        dto.setCurrencyCode(order.getCurrencyCode() != null ? order.getCurrencyCode().trim() : orderFactory.currencyCode());
        dto.setSpecialRequests(booking.getSpecialRequests());
        dto.setStatus(booking.getStatus());
        dto.setFreeCancellationUntil(booking.getPickupDate().minusHours(FREE_CANCELLATION_HOURS));
        dto.setCancellable("pending".equals(booking.getStatus())
                || ("confirmed".equals(booking.getStatus()) && isFreeCancellation(booking, now)));
        if (payment != null) {
            dto.setPaymentId(payment.getId());
            dto.setPaymentStatus(payment.getStatus());
        }
        if ("pending".equals(booking.getStatus())) {
            dto.setHoldExpiresAt(orderFactory.holdExpiresAt(booking.getCreatedAt()));
        }
        UserEntity user = booking.getUser();
        dto.setUserId(user.getId());
        dto.setUserEmail(user.getEmail());
        dto.setUserName(user.getFullName());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setUpdatedAt(booking.getUpdatedAt());
        return dto;
    }

    private static String joinNonBlank(String separator, String... parts) {
        List<String> values = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                values.add(part.trim());
            }
        }
        return String.join(separator, values);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
