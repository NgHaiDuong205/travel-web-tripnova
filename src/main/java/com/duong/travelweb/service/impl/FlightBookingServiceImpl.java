package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.FlightBookingDTO;
import com.duong.travelweb.model.dto.FlightBookingRequestDTO;
import com.duong.travelweb.model.dto.FlightOrderCreatedDTO;
import com.duong.travelweb.model.dto.FlightPassengerRequestDTO;
import com.duong.travelweb.model.dto.InvoiceItemDTO;
import com.duong.travelweb.model.entity.FlightBookingEntity;
import com.duong.travelweb.model.entity.FlightEntity;
import com.duong.travelweb.model.entity.FlightSeatEntity;
import com.duong.travelweb.model.entity.OrderEntity;
import com.duong.travelweb.model.entity.PaymentEntity;
import com.duong.travelweb.model.entity.UserEntity;
import com.duong.travelweb.repository.FlightBookingRepository;
import com.duong.travelweb.repository.FlightRepository;
import com.duong.travelweb.repository.FlightSeatRepository;
import com.duong.travelweb.repository.PaymentRepository;
import com.duong.travelweb.repository.UserRepository;
import com.duong.travelweb.service.FlightBookingService;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FlightBookingServiceImpl implements FlightBookingService {
    private static final Logger log = LoggerFactory.getLogger(FlightBookingServiceImpl.class);
    private static final String EXPIRED_REASON = "Hết thời gian giữ ghế, đơn chưa được thanh toán";
    private static final String TAKEN_REASON = "Ghế đã được khách khác giữ trước khi thanh toán hoàn tất";
    /** Ngừng bán vé trước giờ bay chừng này. */
    private static final Duration SALES_CLOSE_BEFORE = Duration.ofHours(2);
    private static final List<String> ADMIN_REFUNDABLE = List.of("confirmed", "no_show", "checked_in", "completed");

    private final FlightBookingRepository flightBookingRepository;
    private final FlightRepository flightRepository;
    private final FlightSeatRepository flightSeatRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final OrderFactory orderFactory;

    public FlightBookingServiceImpl(FlightBookingRepository flightBookingRepository,
                                    FlightRepository flightRepository,
                                    FlightSeatRepository flightSeatRepository,
                                    PaymentRepository paymentRepository,
                                    UserRepository userRepository,
                                    OrderFactory orderFactory) {
        this.flightBookingRepository = flightBookingRepository;
        this.flightRepository = flightRepository;
        this.flightSeatRepository = flightSeatRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.orderFactory = orderFactory;
    }

    // ================= Khách =================

    @Override
    @Transactional
    public FlightOrderCreatedDTO create(UUID userId, FlightBookingRequestDTO request) {
        FlightEntity flight = flightRepository.findById(request.getFlightId())
                .filter(f -> Boolean.TRUE.equals(f.getIsActive()))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy chuyến bay"));
        requireOnSale(flight);

        Set<UUID> seatIds = new HashSet<>();
        for (FlightPassengerRequestDTO passenger : request.getPassengers()) {
            if (!seatIds.add(passenger.getSeatId())) {
                throw ApiException.badRequest("Mỗi hành khách phải chọn một ghế khác nhau");
            }
        }
        // Khoá các ghế theo thứ tự id: hai người chọn cùng ghế sẽ xếp hàng, người sau thấy ghế đã held.
        Map<UUID, FlightSeatEntity> seats = new HashMap<>();
        for (FlightSeatEntity seat : flightSeatRepository.lockByIds(seatIds)) {
            seats.put(seat.getId(), seat);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (FlightPassengerRequestDTO passenger : request.getPassengers()) {
            FlightSeatEntity seat = seats.get(passenger.getSeatId());
            if (seat == null || !seat.getFlight().getId().equals(flight.getId())) {
                throw ApiException.badRequest("Ghế không thuộc chuyến bay này");
            }
            if (!"available".equals(seat.getStatus())) {
                throw ApiException.conflict("Ghế " + seat.getSeatNumber() + " vừa có người chọn, vui lòng chọn ghế khác");
            }
            total = total.add(seat.getPrice());
        }

        OrderFactory.PendingOrder pending = orderFactory.createPendingOrder(userId, total, request.getPaymentMethod());
        UserEntity user = userRepository.getReferenceById(userId);
        LocalDateTime now = LocalDateTime.now();
        List<FlightBookingEntity> bookings = new ArrayList<>();
        for (FlightPassengerRequestDTO passenger : request.getPassengers()) {
            FlightSeatEntity seat = seats.get(passenger.getSeatId());
            seat.setStatus("held");
            FlightBookingEntity booking = new FlightBookingEntity();
            booking.setOrder(pending.order());
            booking.setUser(user);
            booking.setFlight(flight);
            booking.setSeat(seat);
            booking.setPassengerName(passenger.getPassengerName().trim());
            booking.setPassengerEmail(blankToNull(passenger.getPassengerEmail()));
            booking.setPassengerPhone(blankToNull(passenger.getPassengerPhone()));
            booking.setPassportNo(blankToNull(passenger.getPassportNo()));
            booking.setBaggageKg(passenger.getBaggageKg() == null ? null : passenger.getBaggageKg().shortValue());
            booking.setTotalPrice(seat.getPrice());
            booking.setStatus("pending");
            booking.setCreatedAt(now);
            booking.setUpdatedAt(now);
            bookings.add(flightBookingRepository.save(booking));
        }
        flightRepository.syncSeatCounts(flight.getId());

        FlightOrderCreatedDTO dto = new FlightOrderCreatedDTO();
        dto.setOrderId(pending.order().getId());
        dto.setOrderCode(pending.order().getOrderCode());
        dto.setPaymentId(pending.payment().getId());
        dto.setAmount(total);
        dto.setCurrencyCode(orderFactory.currencyCode());
        dto.setHoldExpiresAt(orderFactory.holdExpiresAt(pending.order().getCreatedAt()));
        dto.setBookings(bookings.stream().map(b -> toDTO(b, pending.payment())).toList());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public FlightBookingDTO get(UUID userId, boolean isAdmin, UUID bookingId) {
        FlightBookingEntity booking = findBooking(bookingId, userId, isAdmin);
        return toDTO(booking, orderFactory.findLatestPayment(booking.getOrder()));
    }

    @Override
    @Transactional
    public FlightBookingDTO cancel(UUID userId, UUID bookingId, String reason) {
        FlightBookingEntity booking = findBooking(bookingId, userId, false);
        String note = reason != null && !reason.isBlank() ? reason.trim() : "Khách hàng huỷ vé";
        switch (booking.getStatus()) {
            case "pending" -> cancelPendingOrder(booking.getOrder(), note);
            case "confirmed" -> {
                if (!isFreeCancellation(booking, LocalDateTime.now())) {
                    throw ApiException.badRequest("Đã quá hạn huỷ miễn phí (" + FREE_CANCELLATION_HOURS
                            + " giờ trước giờ bay), không thể huỷ vé");
                }
                refundBooking(booking, note);
                syncOrderAfterRefund(booking.getOrder(), note);
            }
            default -> throw ApiException.badRequest("Không thể huỷ vé ở trạng thái " + booking.getStatus());
        }
        return toDTO(booking, orderFactory.findLatestPayment(booking.getOrder()));
    }

    @Override
    @Transactional
    public FlightBookingDTO selectSeat(UUID userId, UUID bookingId, UUID seatId) {
        FlightBookingEntity booking = findBooking(bookingId, userId, false);
        String status = booking.getStatus();
        if (!"pending".equals(status) && !"confirmed".equals(status)) {
            throw ApiException.badRequest("Không thể đổi ghế cho vé ở trạng thái " + status);
        }
        requireOnSale(booking.getFlight());
        FlightSeatEntity current = booking.getSeat();
        if (current != null && current.getId().equals(seatId)) {
            return toDTO(booking, orderFactory.findLatestPayment(booking.getOrder()));
        }
        List<UUID> ids = new ArrayList<>();
        ids.add(seatId);
        if (current != null) {
            ids.add(current.getId());
        }
        Map<UUID, FlightSeatEntity> locked = new HashMap<>();
        for (FlightSeatEntity seat : flightSeatRepository.lockByIds(ids)) {
            locked.put(seat.getId(), seat);
        }
        FlightSeatEntity target = locked.get(seatId);
        if (target == null || !target.getFlight().getId().equals(booking.getFlight().getId())) {
            throw ApiException.badRequest("Ghế không thuộc chuyến bay này");
        }
        if (!"available".equals(target.getStatus())) {
            throw ApiException.conflict("Ghế " + target.getSeatNumber() + " đã có người chọn");
        }
        OrderEntity order = booking.getOrder();
        if ("confirmed".equals(status)) {
            if (current != null && (!current.getSeatClass().equals(target.getSeatClass())
                    || current.getPrice().compareTo(target.getPrice()) != 0)) {
                throw ApiException.badRequest("Vé đã thanh toán chỉ đổi được sang ghế cùng hạng, cùng giá");
            }
        } else {
            // Còn pending: tính lại tiền của vé, order và payment đang chờ.
            BigDecimal diff = target.getPrice().subtract(booking.getTotalPrice());
            booking.setTotalPrice(target.getPrice());
            order.setSubtotal(order.getSubtotal().add(diff));
            order.setTotalAmount(order.getTotalAmount().add(diff));
            order.setUpdatedAt(LocalDateTime.now());
            PaymentEntity payment = orderFactory.findLatestPayment(order);
            if (payment != null && "pending".equals(payment.getStatus())) {
                payment.setAmount(order.getTotalAmount());
            }
        }
        if (current != null) {
            releaseSeat(locked.get(current.getId()), booking.getId());
        }
        target.setStatus("confirmed".equals(status) ? "booked" : "held");
        booking.setSeat(target);
        booking.setUpdatedAt(LocalDateTime.now());
        flightRepository.syncSeatCounts(booking.getFlight().getId());
        return toDTO(booking, orderFactory.findLatestPayment(order));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FlightBookingDTO> findMine(UUID userId, String statusGroup, int page, int limit) {
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, limit);
        List<String> statuses = statusesOf(statusGroup);
        Page<FlightBookingEntity> bookings = statuses == null
                ? flightBookingRepository.findByUser(userId, pageable)
                : flightBookingRepository.findByUserAndStatuses(userId, statuses, pageable);
        return new PageImpl<>(toDTOs(bookings.getContent()), pageable, bookings.getTotalElements());
    }

    // ================= Admin =================

    @Override
    @Transactional(readOnly = true)
    public Page<FlightBookingDTO> findForAdmin(String status, String keyword, int page, int limit) {
        List<FlightBookingEntity> bookings = flightBookingRepository.findForAdmin(status, keyword, page, limit);
        long total = flightBookingRepository.countForAdmin(status, keyword);
        return new PageImpl<>(toDTOs(bookings), PageRequest.of(Math.max(page, 1) - 1, limit), total);
    }

    @Override
    @Transactional
    public FlightBookingDTO updateStatusByAdmin(UUID bookingId, String status, String reason) {
        FlightBookingEntity booking = findBooking(bookingId, null, true);
        String next = status == null ? "" : status.trim().toLowerCase();
        String note = reason != null && !reason.isBlank() ? reason.trim() : "Quản trị viên huỷ vé";
        switch (booking.getStatus() + "->" + next) {
            case "pending->cancelled" -> cancelPendingOrder(booking.getOrder(), note);
            case "confirmed->cancelled" -> {
                refundBooking(booking, note);
                syncOrderAfterRefund(booking.getOrder(), note);
            }
            case "confirmed->checked_in", "checked_in->completed", "confirmed->no_show" -> {
                booking.setStatus(next);
                booking.setUpdatedAt(LocalDateTime.now());
            }
            default -> throw ApiException.badRequest("Không thể chuyển trạng thái từ " + booking.getStatus() + " sang " + next);
        }
        return toDTO(booking, orderFactory.findLatestPayment(booking.getOrder()));
    }

    // ================= OrderBookingHandler =================

    @Override
    public String bookingType() {
        return "flight";
    }

    @Override
    @Transactional(readOnly = true)
    public boolean handles(UUID orderId) {
        return flightBookingRepository.existsByOrderId(orderId);
    }

    @Override
    @Transactional
    public boolean confirmOrder(OrderEntity order) {
        LocalDateTime now = LocalDateTime.now();
        List<FlightBookingEntity> bookings = flightBookingRepository.findByOrderId(order.getId());
        List<UUID> seatIds = bookings.stream().filter(b -> b.getSeat() != null).map(b -> b.getSeat().getId()).toList();
        Map<UUID, FlightSeatEntity> locked = new HashMap<>();
        if (!seatIds.isEmpty()) {
            for (FlightSeatEntity seat : flightSeatRepository.lockByIds(seatIds)) {
                locked.put(seat.getId(), seat);
            }
        }
        boolean allConfirmed = true;
        boolean anyConfirmed = false;
        for (FlightBookingEntity booking : bookings) {
            if ("confirmed".equals(booking.getStatus())) {
                anyConfirmed = true;
                continue;
            }
            if (!"pending".equals(booking.getStatus()) && !"cancelled".equals(booking.getStatus())) {
                allConfirmed = false;
                continue;
            }
            // pending, hoặc đã bị huỷ vì hết hạn nhưng tiền về muộn: ghế còn thì giữ lại, mất thì hoàn riêng vé đó.
            FlightSeatEntity seat = booking.getSeat() == null ? null : locked.get(booking.getSeat().getId());
            boolean seatOk = seat != null && !"blocked".equals(seat.getStatus())
                    && !flightBookingRepository.seatTakenByOther(seat.getId(), booking.getId());
            if (seatOk) {
                seat.setStatus("booked");
                booking.setStatus("confirmed");
                anyConfirmed = true;
            } else {
                booking.setStatus("refunded");
                allConfirmed = false;
            }
            booking.setUpdatedAt(now);
        }
        order.setStatus(allConfirmed ? "paid" : anyConfirmed ? "partially_refunded" : "refunded");
        order.setCancelledAt(anyConfirmed ? null : now);
        order.setCancelReason(allConfirmed ? null : TAKEN_REASON);
        order.setUpdatedAt(now);
        bookings.stream().map(b -> b.getFlight().getId()).distinct().forEach(flightRepository::syncSeatCounts);
        return anyConfirmed;
    }

    @Override
    @Transactional
    public void cancelPendingOrder(OrderEntity order, String reason) {
        LocalDateTime now = LocalDateTime.now();
        List<FlightBookingEntity> bookings = flightBookingRepository.findByOrderId(order.getId());
        for (FlightBookingEntity booking : bookings) {
            if ("pending".equals(booking.getStatus())) {
                booking.setStatus("cancelled");
                booking.setUpdatedAt(now);
                if (booking.getSeat() != null) {
                    flightSeatRepository.lockById(booking.getSeat().getId()).ifPresent(seat -> releaseSeat(seat, booking.getId()));
                }
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
        bookings.stream().map(b -> b.getFlight().getId()).distinct().forEach(flightRepository::syncSeatCounts);
    }

    @Override
    @Transactional
    public void refundOrderByAdmin(OrderEntity order, String reason) {
        if (!"paid".equals(order.getStatus()) && !"partially_refunded".equals(order.getStatus())) {
            throw ApiException.badRequest("Đơn hàng chưa thanh toán hoặc đã hoàn tiền, không thể hoàn tiền");
        }
        String note = reason != null && !reason.isBlank() ? reason.trim() : "Quản trị viên hoàn tiền";
        boolean refunded = false;
        for (FlightBookingEntity booking : flightBookingRepository.findByOrderId(order.getId())) {
            if (ADMIN_REFUNDABLE.contains(booking.getStatus())) {
                refundBooking(booking, note);
                refunded = true;
            }
        }
        if (!refunded) {
            throw ApiException.badRequest("Đơn hàng không còn vé nào có thể hoàn tiền");
        }
        syncOrderAfterRefund(order, note);
    }

    @Override
    @Transactional
    public int expirePendingBookings() {
        Map<UUID, OrderEntity> orders = new LinkedHashMap<>();
        for (FlightBookingEntity booking : flightBookingRepository.findPendingCreatedBefore(orderFactory.holdCutoff())) {
            orders.putIfAbsent(booking.getOrder().getId(), booking.getOrder());
        }
        orders.values().forEach(order -> cancelPendingOrder(order, EXPIRED_REASON));
        if (!orders.isEmpty()) {
            log.info("Expired {} pending flight orders", orders.size());
        }
        return orders.size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> bookingIds(UUID orderId) {
        return flightBookingRepository.findByOrderId(orderId).stream().map(FlightBookingEntity::getId).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceItemDTO> invoiceItems(UUID orderId) {
        List<InvoiceItemDTO> items = new ArrayList<>();
        for (FlightBookingEntity booking : flightBookingRepository.findByOrderId(orderId)) {
            FlightEntity flight = booking.getFlight();
            FlightSeatEntity seat = booking.getSeat();
            InvoiceItemDTO item = new InvoiceItemDTO();
            item.setItemType("flight");
            item.setBookingId(booking.getId());
            item.setTitle(flight.getAirline() + " " + flight.getFlightNumber() + " · "
                    + flight.getDepartureAirportCode() + " → " + flight.getArrivalAirportCode());
            item.setSubtitle(booking.getPassengerName()
                    + (seat != null ? " · seat " + seat.getSeatNumber() + " (" + seat.getSeatClass().replace('_', ' ') + ")" : ""));
            item.setCheckInDate(flight.getDepartureTime().toLocalDate());
            item.setCheckOutDate(flight.getArrivalTime().toLocalDate());
            item.setGuests(1);
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
            for (Object[] row : flightBookingRepository.sumRefundByOrderIds(orderIds)) {
                refunds.put((UUID) row[0], (BigDecimal) row[1]);
            }
        }
        return refunds;
    }

    // ================= helpers =================

    /** Hoàn 100% một vé; nhả ghế nếu chuyến chưa bay. */
    private void refundBooking(FlightBookingEntity booking, String reason) {
        boolean notFlownYet = "confirmed".equals(booking.getStatus()) && booking.getFlight().getDepartureTime().isAfter(LocalDateTime.now());
        booking.setStatus("refunded");
        booking.setUpdatedAt(LocalDateTime.now());
        if (notFlownYet && booking.getSeat() != null) {
            flightSeatRepository.lockById(booking.getSeat().getId()).ifPresent(seat -> releaseSeat(seat, booking.getId()));
            flightRepository.syncSeatCounts(booking.getFlight().getId());
        }
    }

    /** Sau khi hoàn một số vé: hoàn hết (hoặc đã huỷ) → order + payment refunded, ngược lại partially_refunded. */
    private void syncOrderAfterRefund(OrderEntity order, String reason) {
        LocalDateTime now = LocalDateTime.now();
        boolean allRefunded = flightBookingRepository.findByOrderId(order.getId()).stream()
                .allMatch(b -> "refunded".equals(b.getStatus()) || "cancelled".equals(b.getStatus()));
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

    /** Trả ghế về available nếu không còn vé nào khác đang giữ nó (ghế blocked giữ nguyên). */
    private void releaseSeat(FlightSeatEntity seat, UUID bookingId) {
        if (seat != null && !"blocked".equals(seat.getStatus()) && !flightBookingRepository.seatTakenByOther(seat.getId(), bookingId)) {
            seat.setStatus("available");
        }
    }

    private void requireOnSale(FlightEntity flight) {
        if (!flight.getDepartureTime().isAfter(LocalDateTime.now().plus(SALES_CLOSE_BEFORE))) {
            throw ApiException.badRequest("Chuyến bay đã đóng bán vé (trước giờ bay " + SALES_CLOSE_BEFORE.toHours() + " giờ)");
        }
    }

    private boolean isFreeCancellation(FlightBookingEntity booking, LocalDateTime now) {
        return now.isBefore(booking.getFlight().getDepartureTime().minusHours(FREE_CANCELLATION_HOURS));
    }

    private FlightBookingEntity findBooking(UUID bookingId, UUID userId, boolean isAdmin) {
        return flightBookingRepository.findDetailById(bookingId)
                .filter(b -> isAdmin || b.getUser().getId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy vé máy bay"));
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

    private List<FlightBookingDTO> toDTOs(List<FlightBookingEntity> bookings) {
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

    private FlightBookingDTO toDTO(FlightBookingEntity booking, PaymentEntity payment) {
        FlightEntity flight = booking.getFlight();
        FlightSeatEntity seat = booking.getSeat();
        OrderEntity order = booking.getOrder();
        FlightBookingDTO dto = new FlightBookingDTO();
        dto.setId(booking.getId());
        dto.setOrderId(order.getId());
        dto.setOrderCode(order.getOrderCode());
        dto.setOrderStatus(order.getStatus());
        dto.setFlightId(flight.getId());
        dto.setFlightNumber(flight.getFlightNumber());
        dto.setAirline(flight.getAirline());
        dto.setAirlineLogoUrl(flight.getAirlineLogoUrl());
        dto.setDepartureAirportCode(flight.getDepartureAirportCode());
        dto.setArrivalAirportCode(flight.getArrivalAirportCode());
        dto.setDepartureCity(flight.getDepartureCity());
        dto.setArrivalCity(flight.getArrivalCity());
        dto.setDepartureTime(flight.getDepartureTime());
        dto.setArrivalTime(flight.getArrivalTime());
        if (seat != null) {
            dto.setSeatId(seat.getId());
            dto.setSeatNumber(seat.getSeatNumber());
            dto.setSeatClass(seat.getSeatClass());
        }
        dto.setPassengerName(booking.getPassengerName());
        dto.setPassengerEmail(booking.getPassengerEmail());
        dto.setPassengerPhone(booking.getPassengerPhone());
        dto.setPassportNo(booking.getPassportNo());
        dto.setBaggageKg(booking.getBaggageKg() == null ? null : booking.getBaggageKg().intValue());
        dto.setTotalPrice(booking.getTotalPrice());
        dto.setCurrencyCode(order.getCurrencyCode() != null ? order.getCurrencyCode().trim() : orderFactory.currencyCode());
        dto.setStatus(booking.getStatus());
        dto.setFreeCancellationUntil(flight.getDepartureTime().minusHours(FREE_CANCELLATION_HOURS));
        dto.setCancellable("pending".equals(booking.getStatus())
                || ("confirmed".equals(booking.getStatus()) && isFreeCancellation(booking, LocalDateTime.now())));
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
