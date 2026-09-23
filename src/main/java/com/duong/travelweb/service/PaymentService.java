package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.PaymentDTO;

import java.util.UUID;

public interface PaymentService {
    PaymentDTO getPayment(UUID userId, UUID paymentId);

    /** Giả lập cổng thanh toán trả kết quả (chỉ dùng khi chưa tích hợp VNPay/MoMo thật). */
    PaymentDTO mockPayment(UUID userId, UUID paymentId, boolean success);

    /** Xử lý kết quả cổng thanh toán (dùng chung cho mock và webhook thật sau này). */
    PaymentDTO handleGatewayResult(UUID paymentId, boolean success, String transactionId, String gatewayResponse);
}
