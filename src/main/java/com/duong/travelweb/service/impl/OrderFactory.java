package com.duong.travelweb.service.impl;

import com.duong.travelweb.model.entity.OrderEntity;
import com.duong.travelweb.model.entity.PaymentEntity;
import com.duong.travelweb.model.entity.UserEntity;
import com.duong.travelweb.repository.OrderRepository;
import com.duong.travelweb.repository.PaymentRepository;
import com.duong.travelweb.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/** Tạo order (pending) + payment (pending) dùng chung cho mọi loại booking; gọi trong transaction của service đặt chỗ. */
@Component
public class OrderFactory {
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Order vừa tạo cùng payment chờ thanh toán. */
    public record PendingOrder(OrderEntity order, PaymentEntity payment) {
    }

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final long holdMinutes;
    private final String currencyCode;
    private final String frontendUrl;

    public OrderFactory(OrderRepository orderRepository,
                        PaymentRepository paymentRepository,
                        UserRepository userRepository,
                        @Value("${app.booking.hold-minutes:15}") long holdMinutes,
                        @Value("${app.booking.currency:USD}") String currencyCode,
                        @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.holdMinutes = holdMinutes;
        this.currencyCode = currencyCode;
        this.frontendUrl = frontendUrl;
    }

    public PendingOrder createPendingOrder(UUID userId, BigDecimal total, String paymentMethod) {
        UserEntity user = userRepository.getReferenceById(userId);
        LocalDateTime now = LocalDateTime.now();

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

        PaymentEntity payment = new PaymentEntity();
        payment.setOrder(order);
        payment.setPaymentMethod(paymentMethod);
        payment.setAmount(total);
        payment.setCurrencyCode(currencyCode);
        payment.setStatus("pending");
        payment.setCreatedAt(now);
        payment = paymentRepository.save(payment);
        return new PendingOrder(order, payment);
    }

    /** Giao dịch mới nhất của order (null nếu chưa có). */
    public PaymentEntity findLatestPayment(OrderEntity order) {
        List<PaymentEntity> payments = paymentRepository.findByOrderIds(List.of(order.getId()));
        return payments.isEmpty() ? null : payments.get(0);
    }

    /** Hết hạn giữ chỗ của booking pending tạo lúc createdAt. */
    public LocalDateTime holdExpiresAt(LocalDateTime createdAt) {
        return createdAt.plusMinutes(holdMinutes);
    }

    /** Thời điểm mà booking pending tạo trước đó đã hết hạn giữ chỗ. */
    public LocalDateTime holdCutoff() {
        return LocalDateTime.now().minusMinutes(holdMinutes);
    }

    public String paymentUrl(PaymentEntity payment) {
        return frontendUrl + "/payment/" + payment.getId();
    }

    public String currencyCode() {
        return currencyCode;
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
