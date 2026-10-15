package com.duong.travelweb.service.impl;

import com.duong.travelweb.model.entity.OrderEntity;
import com.duong.travelweb.model.entity.PaymentEntity;
import com.duong.travelweb.model.entity.UserEntity;
import com.duong.travelweb.repository.OrderRepository;
import com.duong.travelweb.repository.PaymentRepository;
import com.duong.travelweb.repository.UserRepository;
import com.duong.travelweb.service.FraudService;
import com.duong.travelweb.util.RequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
    private static final Logger log = LoggerFactory.getLogger(OrderFactory.class);

    /** Order vừa tạo cùng payment chờ thanh toán. */
    public record PendingOrder(OrderEntity order, PaymentEntity payment) {
    }

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final long holdMinutes;
    private final String currencyCode;
    private final String frontendUrl;
    private final FraudService fraudService;

    public OrderFactory(OrderRepository orderRepository,
                        PaymentRepository paymentRepository,
                        UserRepository userRepository,
                        @Value("${app.booking.hold-minutes:15}") long holdMinutes,
                        @Value("${app.booking.currency:USD}") String currencyCode,
                        @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl,
                        FraudService fraudService) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.holdMinutes = holdMinutes;
        this.currencyCode = currencyCode;
        this.frontendUrl = frontendUrl;
        this.fraudService = fraudService;
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
        scoreAfterCommit(payment.getId());
        return new PendingOrder(order, payment);
    }

    /**
     * Chấm điểm rủi ro sau khi đơn commit (tổng tiền đã chốt, kể cả đơn nhiều dòng). Lỗi chấm điểm chỉ ghi log,
     * không ảnh hưởng đặt chỗ; IP lấy từ request hiện tại (chỉ để đếm, không lưu).
     */
    private void scoreAfterCommit(UUID paymentId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        String clientIp = RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? RequestUtil.getClientIp(attributes.getRequest()) : null;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    fraudService.scorePayment(paymentId, clientIp);
                } catch (RuntimeException ex) {
                    log.warn("Fraud scoring failed for payment {}: {}", paymentId, ex.toString());
                }
            }
        });
    }

    /** Chốt tổng tiền khi order được tạo trước rồi mới thêm từng booking (đơn nhiều dòng từ giỏ hàng). */
    public void applyTotal(PendingOrder pending, BigDecimal total) {
        pending.order().setSubtotal(total);
        pending.order().setTotalAmount(total);
        pending.payment().setAmount(total);
    }

    /** Trang thanh toán chung cho xe / chuyến bay / tour. */
    public String tripPaymentUrl(PaymentEntity payment) {
        return frontendUrl + "/trip-payment/" + payment.getId();
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
