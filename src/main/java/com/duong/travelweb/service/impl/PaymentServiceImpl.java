package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.PaymentDTO;
import com.duong.travelweb.model.entity.OrderEntity;
import com.duong.travelweb.model.entity.PaymentEntity;
import com.duong.travelweb.repository.PaymentRepository;
import com.duong.travelweb.service.OrderBookingHandler;
import com.duong.travelweb.service.InvoiceService;
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
    private final OrderBookingRouter orderBookingRouter;
    private final InvoiceService invoiceService;
    private final boolean mockEnabled;
    private final String currencyCode;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              OrderBookingRouter orderBookingRouter,
                              InvoiceService invoiceService,
                              @Value("${app.payment.mock-enabled:false}") boolean mockEnabled,
                              @Value("${app.booking.currency:USD}") String currencyCode) {
        this.paymentRepository = paymentRepository;
        this.orderBookingRouter = orderBookingRouter;
        this.invoiceService = invoiceService;
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

    /** Map sang DTO, lấy loại booking + booking theo từng order. */
    private Page<PaymentDTO> toDTOPage(Page<PaymentEntity> payments) {
        Map<UUID, BookingRef> refs = new HashMap<>();
        for (PaymentEntity payment : payments) {
            refs.computeIfAbsent(payment.getOrder().getId(), this::bookingRef);
        }
        return payments.map(p -> toDTO(p, refs.get(p.getOrder().getId())));
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
            if (orderBookingRouter.handlerFor(order).confirmOrder(order)) {
                invoiceService.issueForOrder(order, payment);
            } else {
                payment.setStatus("refunded");
            }
        } else if ("pending".equals(payment.getStatus())) {
            payment.setStatus("failed");
            orderBookingRouter.handlerFor(order).cancelPendingOrder(order, "Thanh toán thất bại");
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
        orderBookingRouter.handlerFor(payment.getOrder()).refundOrderByAdmin(payment.getOrder(), reason);
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
        return toDTO(payment, bookingRef(payment.getOrder().getId()));
    }

    /** Loại booking và các booking của order (order luôn chỉ có một loại). */
    private record BookingRef(String type, List<UUID> bookingIds) {
    }

    private BookingRef bookingRef(UUID orderId) {
        for (OrderBookingHandler handler : orderBookingRouter.all()) {
            if (handler.handles(orderId)) {
                return new BookingRef(handler.bookingType(), handler.bookingIds(orderId));
            }
        }
        return new BookingRef(null, List.of());
    }

    /**
     * bookingType = hotel | car | tour | flight. bookingId chỉ điền với hotel (FE cũ chuyển tới /bookings/{id} là trang
     * booking khách sạn), các loại khác dùng primaryBookingId. bookingCount > 1 với đơn đặt từ giỏ hàng.
     */
    private PaymentDTO toDTO(PaymentEntity payment, BookingRef ref) {
        UUID first = ref.bookingIds().isEmpty() ? null : ref.bookingIds().get(0);
        PaymentDTO dto = toDTO(payment, "hotel".equals(ref.type()) ? first : null, ref.bookingIds().size());
        dto.setBookingType(ref.type());
        dto.setPrimaryBookingId(first);
        return dto;
    }

    /** bookingId = booking đầu tiên của order; bookingCount > 1 với đơn đặt từ giỏ hàng. */
    private PaymentDTO toDTO(PaymentEntity payment, UUID bookingId, int bookingCount) {
        OrderEntity order = payment.getOrder();
        PaymentDTO dto = new PaymentDTO();
        dto.setBookingCount(bookingCount);
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
