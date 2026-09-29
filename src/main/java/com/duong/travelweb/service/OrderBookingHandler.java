package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.InvoiceItemDTO;
import com.duong.travelweb.model.entity.OrderEntity;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Phần xử lý order theo từng loại sản phẩm (hotel, car, tour, flight). Mỗi order chỉ chứa booking của MỘT loại,
 * nên thanh toán / hết hạn giữ chỗ / hoàn tiền / hoá đơn chỉ cần tìm đúng handler (xem OrderBookingRouter).
 */
public interface OrderBookingHandler {
    /** hotel | car | tour | flight */
    String bookingType();

    /** Order có booking thuộc loại này không. */
    boolean handles(UUID orderId);

    /** Cổng thanh toán báo thành công: giữ chỗ chính thức. Trả về false nếu không giữ được gì (đã hoàn toàn bộ). */
    boolean confirmOrder(OrderEntity order);

    /** Thanh toán thất bại / hết hạn giữ chỗ / khách huỷ khi chưa trả tiền. */
    void cancelPendingOrder(OrderEntity order, String reason);

    /** Admin hoàn toàn bộ phần còn hoàn được của order (hoàn tiền theo giao dịch). */
    void refundOrderByAdmin(OrderEntity order, String reason);

    /** Huỷ các booking pending quá hạn giữ chỗ, trả về số booking đã huỷ. */
    int expirePendingBookings();

    /** Id các booking của order (thứ tự ổn định). */
    List<UUID> bookingIds(UUID orderId);

    /** Các dòng hoá đơn của order. */
    List<InvoiceItemDTO> invoiceItems(UUID orderId);

    /** orderId -> tổng tiền đã hoàn (chỉ các order thuộc loại này). */
    Map<UUID, BigDecimal> refundedAmounts(Collection<UUID> orderIds);
}
