package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.InvoiceBillingRequestDTO;
import com.duong.travelweb.model.dto.InvoiceDTO;
import com.duong.travelweb.model.entity.OrderEntity;
import com.duong.travelweb.model.entity.PaymentEntity;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.UUID;

/** Hoá đơn: 1 order = 1 hoá đơn, phát hành khi thanh toán thành công (có ít nhất một booking được giữ phòng). */
public interface InvoiceService {
    /** File PDF để tải về. */
    record InvoiceFile(String fileName, byte[] content) {
    }

    /** Idempotent: order đã có hoá đơn thì bỏ qua. Chạy trong transaction của thanh toán. */
    void issueForOrder(OrderEntity order, PaymentEntity payment);

    /** Bổ sung hoá đơn cho các order đã thanh toán từ trước khi có tính năng này. Trả về số hoá đơn đã tạo. */
    int backfillMissing();

    Page<InvoiceDTO> findMine(UUID userId, int page, int limit);

    InvoiceDTO getMine(UUID userId, UUID invoiceId);

    InvoiceFile downloadMine(UUID userId, UUID invoiceId);

    /** Khách cập nhật thông tin xuất hoá đơn (tên / địa chỉ / MST); PDF tải sau đó dùng thông tin mới. */
    InvoiceDTO updateMyBilling(UUID userId, UUID invoiceId, InvoiceBillingRequestDTO request);

    Page<InvoiceDTO> findForAdmin(String keyword, LocalDate from, LocalDate to, int page, int limit);

    InvoiceDTO get(UUID invoiceId);

    InvoiceFile download(UUID invoiceId);

    /** Gửi lại hoá đơn qua email (chưa có mail server -> ghi log). */
    InvoiceDTO resend(UUID invoiceId);
}
