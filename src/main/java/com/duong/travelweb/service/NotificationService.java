package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.NotificationDTO;
import com.duong.travelweb.model.entity.OrderEntity;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface NotificationService {
    /** Tạo thông báo cho user (chạy trong transaction của nghiệp vụ gọi tới). userId null -> bỏ qua. */
    void notify(UUID userId, String type, String title, String body, String link, String entityType, UUID entityId);

    /** Thông báo cho chủ order, link tới trang booking theo loại (hotel/car/flight/tour). */
    void notifyOrder(OrderEntity order, String type, String title, String body);

    /**
     * Báo cho khách khi admin đổi trạng thái booking. bookingType = hotel|car|flight|tour, status = trạng thái MỚI của
     * booking (cancelled / refunded / checked_in / checked_out / completed / no_show); reason do admin nhập (có thể null).
     */
    void notifyBookingStatus(UUID userId, String bookingType, UUID bookingId, String status, String reason);

    Page<NotificationDTO> findMine(UUID userId, boolean unreadOnly, int page, int limit);

    long countUnread(UUID userId);

    NotificationDTO markRead(UUID userId, UUID notificationId);

    /** Trả về số thông báo vừa được đánh dấu đã đọc. */
    int markAllRead(UUID userId);

    void delete(UUID userId, UUID notificationId);
}
