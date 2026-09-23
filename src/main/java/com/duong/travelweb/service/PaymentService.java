package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.PaymentDTO;
import com.duong.travelweb.model.dto.PaymentSummaryDTO;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface PaymentService {
    PaymentDTO getPayment(UUID userId, UUID paymentId);

    /** @param status pending | success | failed | refunded (null = tất cả); page bắt đầu từ 1 */
    Page<PaymentDTO> findMyPayments(UUID userId, String status, int page, int limit);

    PaymentSummaryDTO getMySummary(UUID userId);

    /** Admin: tất cả giao dịch, lọc theo trạng thái. */
    Page<PaymentDTO> findAllForAdmin(String status, int page, int limit);

    /** Giả lập cổng thanh toán trả kết quả (chỉ dùng khi chưa tích hợp VNPay/MoMo thật). */
    PaymentDTO mockPayment(UUID userId, UUID paymentId, boolean success);

    /** Xử lý kết quả cổng thanh toán (dùng chung cho mock và webhook thật sau này). */
    PaymentDTO handleGatewayResult(UUID paymentId, boolean success, String transactionId, String gatewayResponse);
}
