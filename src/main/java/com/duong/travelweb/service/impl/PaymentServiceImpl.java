package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.PaymentDTO;
import com.duong.travelweb.model.entity.HotelBookingEntity;
import com.duong.travelweb.model.entity.OrderEntity;
import com.duong.travelweb.model.entity.PaymentEntity;
import com.duong.travelweb.repository.HotelBookingRepository;
import com.duong.travelweb.repository.PaymentRepository;
import com.duong.travelweb.service.HotelBookingService;
import com.duong.travelweb.service.PaymentService;
import com.duong.travelweb.util.SecurityUtil;
import com.duong.travelweb.model.dto.PaymentSummaryDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final HotelBookingRepository hotelBookingRepository;
    private final HotelBookingService hotelBookingService;
    private final boolean mockEnabled;
    private final String currencyCode;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              HotelBookingRepository hotelBookingRepository,
                              HotelBookingService hotelBookingService,
                              @Value("${app.payment.mock-enabled:false}") boolean mockEnabled,
                              @Value("${app.booking.currency:USD}") String currencyCode) {
        this.paymentRepository = paymentRepository;
        this.hotelBookingRepository = hotelBookingRepository;
        this.hotelBookingService = hotelBookingService;
        this.mockEnabled = mockEnabled;
        this.currencyCode = currencyCode;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDTO getPayment(UUID userId, UUID paymentId) {
        return toDTO(findOwnedPayment(userId, paymentId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentDTO> findMyPayments(UUID userId, String status, int page, int limit) {
        String filter = normalizeStatus(status);
        PageRequest pageable = PageRequest.of(Math.max(page, 1) - 1, limit);
        Page<PaymentEntity> payments = filter == null
                ? paymentRepository.findByUser(userId, pageable)
                : paymentRepository.findByUserAndStatus(userId, filter, pageable);
        return toDTOPage(payments);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentDTO> findAllForAdmin(String status, int page, int limit) {
        String filter = normalizeStatus(status);
        PageRequest pageable = PageRequest.of(Math.max(page, 1) - 1, limit);
        Page<PaymentEntity> payments = filter == null
                ? paymentRepository.findAllForAdmin(pageable)
                : paymentRepository.findAllForAdminByStatus(filter, pageable);
        return toDTOPage(payments);
    }

    private String normalizeStatus(String status) {
        String filter = status == null || status.isBlank() || "all".equals(status) ? null : status;
        if (filter != null && !List.of("pending", "success", "failed", "refunded").contains(filter)) {
            throw ApiException.badRequest("Trạng thái lọc không hợp lệ: " + status);
        }
        return filter;
    }

    /** Map sang DTO, lấy bookingId theo lô. */
    private Page<PaymentDTO> toDTOPage(Page<PaymentEntity> payments) {
        List<UUID> orderIds = payments.stream().map(p -> p.getOrder().getId()).distinct().toList();
        Map<UUID, UUID> bookingIdByOrder = new HashMap<>();
        if (!orderIds.isEmpty()) {
            for (Object[] row : hotelBookingRepository.findBookingIdsByOrderIds(orderIds)) {
                bookingIdByOrder.putIfAbsent((UUID) row[0], (UUID) row[1]);
            }
        }
        return payments.map(p -> toDTO(p, bookingIdByOrder.get(p.getOrder().getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentSummaryDTO getMySummary(UUID userId) {
        Object[] row = paymentRepository.summarizeByUser(userId).get(0);
        PaymentSummaryDTO dto = new PaymentSummaryDTO();
        dto.setTotalTransactions((Long) row[0]);
        dto.setTotalPaid(toBigDecimal(row[1]));
        dto.setTotalRefunded(toBigDecimal(row[2]));
        dto.setCurrencyCode(currencyCode);
        return dto;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }

    @Override
    @Transactional
    public PaymentDTO mockPayment(UUID userId, UUID paymentId, boolean success) {
        if (!mockEnabled) {
            throw ApiException.notFound("Cổng thanh toán giả lập đang tắt");
        }
        findOwnedPayment(userId, paymentId);
        String transactionId = success ? "MOCK-" + UUID.randomUUID() : null;
        String gatewayResponse = "{\"provider\":\"mock\",\"success\":" + success + "}";
        return handleGatewayResult(paymentId, success, transactionId, gatewayResponse);
    }

    @Override
    @Transactional
    public PaymentDTO handleGatewayResult(UUID paymentId, boolean success, String transactionId, String gatewayResponse) {
        PaymentEntity payment = paymentRepository.lockById(paymentId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch thanh toán"));
        // Callback lặp lại (IPN gửi nhiều lần) -> trả kết quả cũ, không xử lý lại.
        if (!"pending".equals(payment.getStatus()) && !"failed".equals(payment.getStatus())) {
            return toDTO(payment);
        }
        OrderEntity order = payment.getOrder();
        payment.setGatewayResponse(gatewayResponse);

        if (success) {
            payment.setStatus("success");
            payment.setTransactionId(transactionId);
            payment.setPaidAt(LocalDateTime.now());
            if (!hotelBookingService.confirmOrder(order)) {
                payment.setStatus("refunded");
            }
        } else if ("pending".equals(payment.getStatus())) {
            payment.setStatus("failed");
            hotelBookingService.cancelPendingOrder(order, "Thanh toán thất bại");
        }
        return toDTO(payment);
    }

    @Override
    @Transactional
    public PaymentDTO updateStatusByAdmin(UUID adminId, UUID paymentId, String status, String reason) {
        PaymentEntity payment = paymentRepository.findDetailById(paymentId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch thanh toán"));
        String gatewayResponse = "{\"provider\":\"manual\",\"adminId\":\"" + adminId + "\"}";
        return switch (payment.getStatus() + "->" + status) {
            case "pending->success" -> handleGatewayResult(paymentId, true, "MANUAL-" + UUID.randomUUID(), gatewayResponse);
            case "pending->failed" -> handleGatewayResult(paymentId, false, null, gatewayResponse);
            case "success->refunded" -> refundByAdmin(paymentId, reason);
            default -> throw ApiException.badRequest(
                    "Không thể chuyển giao dịch từ " + payment.getStatus() + " sang " + status);
        };
    }

    @Override
    @Transactional
    public PaymentDTO refundByAdmin(UUID paymentId, String reason) {
        PaymentEntity payment = paymentRepository.lockById(paymentId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch thanh toán"));
        if (!"success".equals(payment.getStatus())) {
            throw ApiException.badRequest("Chỉ hoàn tiền được giao dịch đã thanh toán thành công");
        }
        hotelBookingService.refundOrderByAdmin(payment.getOrder(), reason);
        return toDTO(payment);
    }

    private PaymentEntity findOwnedPayment(UUID userId, UUID paymentId) {
        PaymentEntity payment = paymentRepository.findDetailById(paymentId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch thanh toán"));
        if (!payment.getOrder().getUser().getId().equals(userId) && !SecurityUtil.hasRole("ADMIN")) {
            throw ApiException.notFound("Không tìm thấy giao dịch thanh toán");
        }
        return payment;
    }

    private PaymentDTO toDTO(PaymentEntity payment) {
        List<HotelBookingEntity> bookings = hotelBookingRepository.findByOrderId(payment.getOrder().getId());
        return toDTO(payment, bookings.isEmpty() ? null : bookings.get(0).getId());
    }

    private PaymentDTO toDTO(PaymentEntity payment, UUID bookingId) {
        OrderEntity order = payment.getOrder();
        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        dto.setOrderId(order.getId());
        dto.setOrderCode(order.getOrderCode());
        dto.setOrderStatus(order.getStatus());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setAmount(payment.getAmount());
        dto.setCurrencyCode(payment.getCurrencyCode());
        dto.setStatus(payment.getStatus());
        dto.setTransactionId(payment.getTransactionId());
        dto.setPaidAt(payment.getPaidAt());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setBookingId(bookingId);
        if (order.getUser() != null) {
            dto.setUserEmail(order.getUser().getEmail());
        }
        return dto;
    }
}
