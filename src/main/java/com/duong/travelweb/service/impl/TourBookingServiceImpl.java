package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.InvoiceItemDTO;
import com.duong.travelweb.model.dto.TourBookingDTO;
import com.duong.travelweb.model.dto.TourBookingRequestDTO;
import com.duong.travelweb.model.entity.OrderEntity;
import com.duong.travelweb.model.entity.PaymentEntity;
import com.duong.travelweb.model.entity.TourBookingEntity;
import com.duong.travelweb.model.entity.TourEntity;
import com.duong.travelweb.model.entity.UserEntity;
import com.duong.travelweb.repository.PaymentRepository;
import com.duong.travelweb.repository.TourBookingRepository;
import com.duong.travelweb.repository.TourRepository;
import com.duong.travelweb.repository.UserRepository;
import com.duong.travelweb.service.NotificationService;
import com.duong.travelweb.service.TourBookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TourBookingServiceImpl implements TourBookingService {
    private static final Logger log = LoggerFactory.getLogger(TourBookingServiceImpl.class);
    private static final String EXPIRED_REASON = "Hết thời gian giữ chỗ, đơn chưa được thanh toán";
    private static final String FULL_REASON = "Tour đã hết chỗ trước khi thanh toán hoàn tất";
    private static final List<String> ADMIN_REFUNDABLE = List.of("confirmed", "no_show", "checked_in", "completed");

    private final NotificationService notificationService;
    private final TourBookingRepository tourBookingRepository;
    private final TourRepository tourRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final OrderFactory orderFactory;

    public TourBookingServiceImpl(TourBookingRepository tourBookingRepository,
                                  TourRepository tourRepository,
                                  PaymentRepository paymentRepository,
                                  UserRepository userRepository,
                                  OrderFactory orderFactory,
                                  NotificationService notificationService) {
        this.tourBookingRepository = tourBookingRepository;
        this.tourRepository = tourRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.orderFactory = orderFactory;
        this.notificationService = notificationService;
    }

    // ================= Khách =================

    @Override
    @Transactional
    public TourBookingDTO create(UUID userId, TourBookingRequestDTO request) {
        // Khoá tour: hai người cùng đặt chỗ cuối sẽ xếp hàng, người sau thấy chỗ đã được giữ.
        TourEntity tour = tourRepository.lockById(request.getTourId())
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy tour"));
        LocalDate departure = request.getDepartureDate();
        if (tour.getDepartureDate() != null) {
            if (!tour.getDepartureDate().equals(departure)) {
                throw ApiException.badRequest("Tour này chỉ khởi hành ngày " + tour.getDepartureDate());
            }
            if (departure.isBefore(LocalDate.now().plusDays(1))) {
                throw ApiException.badRequest("Tour đã đóng nhận khách cho ngày khởi hành này");
            }
        } else if (departure.isBefore(LocalDate.now().plusDays(MIN_LEAD_DAYS))) {
            throw ApiException.badRequest("Vui lòng đặt tour trước ngày khởi hành ít nhất " + MIN_LEAD_DAYS + " ngày");
        }
        int adults = request.getNumAdults();
        int children = request.getNumChildren() == null ? 0 : request.getNumChildren();
        int guests = adults + children;
        if (tour.getMaxParticipants() != null) {
            long booked = tourBookingRepository.countBookedGuests(tour.getId(), departure, null, orderFactory.holdCutoff());
            long left = tour.getMaxParticipants() - booked;
            if (guests > left) {
                throw ApiException.conflict(left <= 0
                        ? "Tour đã hết chỗ cho ngày khởi hành này"
                        : "Tour chỉ còn " + left + " chỗ cho ngày khởi hành này");
            }
        }

        BigDecimal childPrice = tour.getPriceChild() != null ? tour.getPriceChild() : tour.getPriceAdult();
        BigDecimal total = tour.getPriceAdult().multiply(BigDecimal.valueOf(adults))
                .add(childPrice.multiply(BigDecimal.valueOf(children)));
        OrderFactory.PendingOrder pending = orderFactory.createPendingOrder(userId, total, request.getPaymentMethod());
        LocalDateTime now = LocalDateTime.now();
        TourBookingEntity booking = new TourBookingEntity();
        booking.setOrder(pending.order());
        booking.setUser(userRepository.getReferenceById(userId));
        booking.setTour(tour);
        booking.setNumAdults((short) adults);
        booking.setNumChildren((short) children);
        booking.setDepartureDate(departure);
        booking.setContactName(request.getContactName().trim());
        booking.setContactPhone(blankToNull(request.getContactPhone()));
        booking.setContactEmail(blankToNull(request.getContactEmail()));
        booking.setTotalPrice(total);
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setSpecialRequests(blankToNull(request.getSpecialRequests()));
        booking.setStatus("pending");
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);
        return toDTO(tourBookingRepository.save(booking), pending.payment());
    }

    @Override
    @Transactional(readOnly = true)
    public TourBookingDTO get(UUID userId, boolean isAdmin, UUID bookingId) {
        TourBookingEntity booking = findBooking(bookingId, userId, isAdmin);
        return toDTO(booking, orderFactory.findLatestPayment(booking.getOrder()));
    }

    @Override
    @Transactional
    public TourBookingDTO cancel(UUID userId, UUID bookingId, String reason) {
        TourBookingEntity booking = findBooking(bookingId, userId, false);
        String note = reason != null && !reason.isBlank() ? reason.trim() : "Khách hàng huỷ tour";
        switch (booking.getStatus()) {
            case "pending" -> cancelPendingOrder(booking.getOrder(), note);
            case "confirmed" -> {
                if (!isFreeCancellation(booking, LocalDate.now())) {
                    throw ApiException.badRequest("Đã quá hạn huỷ miễn phí (" + FREE_CANCELLATION_DAYS
                            + " ngày trước khởi hành), không thể huỷ tour");
                }
                refundBooking(booking, note);
            }
            default -> throw ApiException.badRequest("Không thể huỷ tour ở trạng thái " + booking.getStatus());
        }
        return toDTO(booking, orderFactory.findLatestPayment(booking.getOrder()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TourBookingDTO> findMine(UUID userId, String statusGroup, int page, int limit) {
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, limit);
        List<String> statuses = statusesOf(statusGroup);
        Page<TourBookingEntity> bookings = statuses == null
                ? tourBookingRepository.findByUser(userId, pageable)
                : tourBookingRepository.findByUserAndStatuses(userId, statuses, pageable);
        return new PageImpl<>(toDTOs(bookings.getContent()), pageable, bookings.getTotalElements());
    }

    // ================= Admin =================

    @Override
    @Transactional(readOnly = true)
    public Page<TourBookingDTO> findForAdmin(String status, String keyword, int page, int limit) {
        List<TourBookingEntity> bookings = tourBookingRepository.findForAdmin(status, keyword, page, limit);
        long total = tourBookingRepository.countForAdmin(status, keyword);
        return new PageImpl<>(toDTOs(bookings), PageRequest.of(Math.max(page, 1) - 1, limit), total);
    }

    @Override
    @Transactional
    public TourBookingDTO updateStatusByAdmin(UUID bookingId, String status, String reason) {
        TourBookingEntity booking = findBooking(bookingId, null, true);
        String next = status == null ? "" : status.trim().toLowerCase();
        String note = reason != null && !reason.isBlank() ? reason.trim() : "Quản trị viên huỷ tour";
        switch (booking.getStatus() + "->" + next) {
            case "pending->cancelled" -> cancelPendingOrder(booking.getOrder(), note);
            case "confirmed->cancelled" -> refundBooking(booking, note);
            case "confirmed->checked_in", "checked_in->completed", "confirmed->no_show" -> {
                booking.setStatus(next);
                booking.setUpdatedAt(LocalDateTime.now());
            }
            default -> throw ApiException.badRequest("Không thể chuyển trạng thái từ " + booking.getStatus() + " sang " + next);
        }
        notificationService.notifyBookingStatus(booking.getUser().getId(), "tour", booking.getId(), booking.getStatus(), reason);
        return toDTO(booking, orderFactory.findLatestPayment(booking.getOrder()));
    }

    // ================= OrderBookingHandler =================

    @Override
    public String bookingType() {
        return "tour";
    }

    @Override
    @Transactional(readOnly = true)
    public boolean handles(UUID orderId) {
        return tourBookingRepository.existsByOrderId(orderId);
    }

    @Override
    @Transactional
    public boolean confirmOrder(OrderEntity order) {
        LocalDateTime now = LocalDateTime.now();
        boolean anyConfirmed = false;
        for (TourBookingEntity booking : tourBookingRepository.findByOrderId(order.getId())) {
            if ("confirmed".equals(booking.getStatus())) {
                anyConfirmed = true;
                continue;
            }
            // pending, hoặc đã bị huỷ vì hết hạn nhưng tiền về muộn: còn đủ chỗ thì giữ, không thì hoàn tiền.
            TourEntity tour = tourRepository.lockById(booking.getTour().getId()).orElseThrow();
            boolean full = false;
            if (tour.getMaxParticipants() != null) {
                long others = tourBookingRepository.countBookedGuests(tour.getId(), booking.getDepartureDate(),
                        booking.getId(), orderFactory.holdCutoff());
                full = others + guestsOf(booking) > tour.getMaxParticipants();
            }
            booking.setStatus(full ? "refunded" : "confirmed");
            booking.setUpdatedAt(now);
            anyConfirmed |= !full;
        }
        order.setStatus(anyConfirmed ? "paid" : "refunded");
        order.setCancelledAt(anyConfirmed ? null : now);
        order.setCancelReason(anyConfirmed ? null : FULL_REASON);
        order.setUpdatedAt(now);
        return anyConfirmed;
    }

    @Override
    @Transactional
    public void cancelPendingOrder(OrderEntity order, String reason) {
        LocalDateTime now = LocalDateTime.now();
        for (TourBookingEntity booking : tourBookingRepository.findByOrderId(order.getId())) {
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
        for (TourBookingEntity booking : tourBookingRepository.findByOrderId(order.getId())) {
            if (ADMIN_REFUNDABLE.contains(booking.getStatus())) {
                refundBooking(booking, note);
                refunded = true;
            }
        }
        if (!refunded) {
            throw ApiException.badRequest("Đơn hàng không còn tour nào có thể hoàn tiền");
        }
    }

    @Override
    @Transactional
    public int expirePendingBookings() {
        List<TourBookingEntity> expired = tourBookingRepository.findPendingCreatedBefore(orderFactory.holdCutoff());
        for (TourBookingEntity booking : expired) {
            cancelPendingOrder(booking.getOrder(), EXPIRED_REASON);
        }
        if (!expired.isEmpty()) {
            log.info("Expired {} pending tour bookings", expired.size());
        }
        return expired.size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> bookingIds(UUID orderId) {
        return tourBookingRepository.findByOrderId(orderId).stream().map(TourBookingEntity::getId).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceItemDTO> invoiceItems(UUID orderId) {
        List<InvoiceItemDTO> items = new ArrayList<>();
        for (TourBookingEntity booking : tourBookingRepository.findByOrderId(orderId)) {
            TourEntity tour = booking.getTour();
            int children = booking.getNumChildren() == null ? 0 : booking.getNumChildren();
            InvoiceItemDTO item = new InvoiceItemDTO();
            item.setItemType("tour");
            item.setBookingId(booking.getId());
            item.setTitle(tour.getName());
            item.setSubtitle(booking.getNumAdults() + " adult" + (booking.getNumAdults() == 1 ? "" : "s")
                    + (children > 0 ? ", " + children + " child" + (children == 1 ? "" : "ren") : "")
                    + " · contact " + booking.getContactName());
            item.setCheckInDate(booking.getDepartureDate());
            item.setCheckOutDate(returnDateOf(booking));
            item.setNights(tour.getDurationDays() == null ? null : tour.getDurationDays().intValue());
            item.setGuests(guestsOf(booking));
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
            for (Object[] row : tourBookingRepository.sumRefundByOrderIds(orderIds)) {
                refunds.put((UUID) row[0], (BigDecimal) row[1]);
            }
        }
        return refunds;
    }

    // ================= helpers =================

    /** Hoàn 100% (1 order = 1 đơn tour): order + payment chuyển refunded, lý do lưu ở orders.cancel_reason. */
    private void refundBooking(TourBookingEntity booking, String reason) {
        LocalDateTime now = LocalDateTime.now();
        booking.setStatus("refunded");
        booking.setUpdatedAt(now);
        OrderEntity order = booking.getOrder();
        boolean allRefunded = tourBookingRepository.findByOrderId(order.getId()).stream()
                .allMatch(b -> b.getId().equals(booking.getId()) || "refunded".equals(b.getStatus()) || "cancelled".equals(b.getStatus()));
        order.setStatus(allRefunded ? "refunded" : "partially_refunded");
        order.setCancelReason(reason);
        order.setUpdatedAt(now);
        if (allRefunded) {
            order.setCancelledAt(now);
            PaymentEntity payment = orderFactory.findLatestPayment(order);
            if (payment != null && "success".equals(payment.getStatus())) {
                payment.setStatus("refunded");
            }
        }
    }

    private boolean isFreeCancellation(TourBookingEntity booking, LocalDate today) {
        return today.isBefore(booking.getDepartureDate().minusDays(FREE_CANCELLATION_DAYS));
    }

    private static int guestsOf(TourBookingEntity booking) {
        return booking.getNumAdults() + (booking.getNumChildren() == null ? 0 : booking.getNumChildren());
    }

    private static LocalDate returnDateOf(TourBookingEntity booking) {
        Short days = booking.getTour().getDurationDays();
        return booking.getDepartureDate().plusDays(days == null ? 0 : Math.max(0, days - 1));
    }

    private TourBookingEntity findBooking(UUID bookingId, UUID userId, boolean isAdmin) {
        return tourBookingRepository.findDetailById(bookingId)
                .filter(b -> isAdmin || b.getUser().getId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn đặt tour"));
    }

    private List<String> statusesOf(String statusGroup) {
        if (statusGroup == null || statusGroup.isBlank() || "all".equals(statusGroup)) {
            return null;
        }
        return switch (statusGroup) {
            case "upcoming" -> List.of("confirmed", "checked_in");
            case "pending" -> List.of("pending");
            case "completed" -> List.of("completed", "no_show");
            case "cancelled" -> List.of("cancelled", "refunded");
            default -> throw ApiException.badRequest("Trạng thái lọc không hợp lệ: " + statusGroup);
        };
    }

    private List<TourBookingDTO> toDTOs(List<TourBookingEntity> bookings) {
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

    private TourBookingDTO toDTO(TourBookingEntity booking, PaymentEntity payment) {
        TourEntity tour = booking.getTour();
        OrderEntity order = booking.getOrder();
        TourBookingDTO dto = new TourBookingDTO();
        dto.setId(booking.getId());
        dto.setOrderId(order.getId());
        dto.setOrderCode(order.getOrderCode());
        dto.setOrderStatus(order.getStatus());
        dto.setTourId(tour.getId());
        dto.setTourName(tour.getName());
        dto.setTourImageUrl(tour.getCoverImageUrl());
        dto.setDestinationName(tour.getDestination() != null ? tour.getDestination().getName() : null);
        dto.setDepartureDate(booking.getDepartureDate());
        dto.setReturnDate(returnDateOf(booking));
        dto.setDurationDays(tour.getDurationDays() == null ? null : tour.getDurationDays().intValue());
        dto.setNumAdults(booking.getNumAdults() == null ? null : booking.getNumAdults().intValue());
        dto.setNumChildren(booking.getNumChildren() == null ? 0 : booking.getNumChildren().intValue());
        dto.setContactName(booking.getContactName());
        dto.setContactPhone(booking.getContactPhone());
        dto.setContactEmail(booking.getContactEmail());
        dto.setTotalPrice(booking.getTotalPrice());
        dto.setCurrencyCode(order.getCurrencyCode() != null ? order.getCurrencyCode().trim() : orderFactory.currencyCode());
        dto.setSpecialRequests(booking.getSpecialRequests());
        dto.setStatus(booking.getStatus());
        dto.setFreeCancellationUntil(booking.getDepartureDate().minusDays(FREE_CANCELLATION_DAYS));
        dto.setCancellable("pending".equals(booking.getStatus())
                || ("confirmed".equals(booking.getStatus()) && isFreeCancellation(booking, LocalDate.now())));
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
