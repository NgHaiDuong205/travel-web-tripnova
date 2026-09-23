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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final HotelBookingRepository hotelBookingRepository;
    private final HotelBookingService hotelBookingService;
    private final boolean mockEnabled;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              HotelBookingRepository hotelBookingRepository,
                              HotelBookingService hotelBookingService,
                              @Value("${app.payment.mock-enabled:false}") boolean mockEnabled) {
        this.paymentRepository = paymentRepository;
        this.hotelBookingRepository = hotelBookingRepository;
        this.hotelBookingService = hotelBookingService;
        this.mockEnabled = mockEnabled;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDTO getPayment(UUID userId, UUID paymentId) {
        return toDTO(findOwnedPayment(userId, paymentId));
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

    private PaymentEntity findOwnedPayment(UUID userId, UUID paymentId) {
        PaymentEntity payment = paymentRepository.findDetailById(paymentId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch thanh toán"));
        if (!payment.getOrder().getUser().getId().equals(userId) && !SecurityUtil.hasRole("ADMIN")) {
            throw ApiException.notFound("Không tìm thấy giao dịch thanh toán");
        }
        return payment;
    }

    private PaymentDTO toDTO(PaymentEntity payment) {
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
        List<HotelBookingEntity> bookings = hotelBookingRepository.findByOrderId(order.getId());
        if (!bookings.isEmpty()) {
            dto.setBookingId(bookings.get(0).getId());
        }
        return dto;
    }
}
