package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.NotificationDTO;
import com.duong.travelweb.model.entity.NotificationEntity;
import com.duong.travelweb.model.entity.OrderEntity;
import com.duong.travelweb.repository.NotificationRepository;
import com.duong.travelweb.service.NotificationService;
import com.duong.travelweb.service.OrderBookingHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements NotificationService {
    private static final int MAX_TITLE = 200;

    private final NotificationRepository notificationRepository;
    /** Lấy lười: các booking service (handler của router) lại phụ thuộc NotificationService -> tránh vòng lặp bean. */
    private final ObjectProvider<OrderBookingRouter> orderBookingRouter;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   ObjectProvider<OrderBookingRouter> orderBookingRouter) {
        this.notificationRepository = notificationRepository;
        this.orderBookingRouter = orderBookingRouter;
    }

    @Override
    @Transactional
    public void notify(UUID userId, String type, String title, String body, String link, String entityType, UUID entityId) {
        if (userId == null) {
            return;
        }
        NotificationEntity notification = new NotificationEntity();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title.length() > MAX_TITLE ? title.substring(0, MAX_TITLE - 1) + "…" : title);
        notification.setBody(body);
        notification.setLink(link);
        notification.setEntityType(entityType);
        notification.setEntityId(entityId);
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void notifyOrder(OrderEntity order, String type, String title, String body) {
        if (order.getUser() == null) {
            return;
        }
        String link = "/my-trips";
        String entityType = "order";
        UUID entityId = order.getId();
        for (OrderBookingHandler handler : orderBookingRouter.getObject().all()) {
            List<UUID> bookingIds = handler.bookingIds(order.getId());
            if (bookingIds.isEmpty()) {
                continue;
            }
            String bookingType = handler.bookingType();
            // Khách sạn có trang chi tiết riêng; xe/chuyến bay/tour nằm trong My Trips (tab + đánh dấu đơn).
            link = "hotel".equals(bookingType)
                    ? "/bookings/" + bookingIds.get(0)
                    : "/my-trips?tab=" + bookingType + "s&paid=" + bookingIds.get(0);
            entityType = bookingType + "_booking";
            entityId = bookingIds.get(0);
            break;
        }
        notify(order.getUser().getId(), type, title, body, link, entityType, entityId);
    }

    @Override
    @Transactional
    public void notifyBookingStatus(UUID userId, String bookingType, UUID bookingId, String status, String reason) {
        String label = switch (bookingType) {
            case "car" -> "car rental";
            case "flight" -> "flight ticket";
            default -> bookingType + " booking";
        };
        String title = switch (status) {
            case "cancelled" -> "Your " + label + " was cancelled";
            case "refunded" -> "Your " + label + " was cancelled and refunded";
            case "checked_in" -> "You are checked in for your " + label;
            case "checked_out" -> "Checked out - thanks for staying with us";
            case "completed" -> "Your " + label + " is completed - share a review!";
            case "no_show" -> "Your " + label + " was marked as no-show";
            default -> "Your " + label + " was updated";
        };
        String link = "hotel".equals(bookingType)
                ? "/bookings/" + bookingId
                : "/my-trips?tab=" + bookingType + "s&paid=" + bookingId;
        String body = reason != null && !reason.isBlank() ? "Reason: " + reason.trim() : null;
        notify(userId, "booking_" + status, title, body, link, bookingType + "_booking", bookingId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDTO> findMine(UUID userId, boolean unreadOnly, int page, int limit) {
        PageRequest pageable = PageRequest.of(Math.max(page, 1) - 1, limit);
        Page<NotificationEntity> result = unreadOnly
                ? notificationRepository.findUnreadByUserId(userId, pageable)
                : notificationRepository.findByUserId(userId, pageable);
        return result.map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationRepository.countUnread(userId);
    }

    @Override
    @Transactional
    public NotificationDTO markRead(UUID userId, UUID notificationId) {
        NotificationEntity notification = findOwned(userId, notificationId);
        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
        }
        return toDTO(notification);
    }

    @Override
    @Transactional
    public int markAllRead(UUID userId) {
        return notificationRepository.markAllRead(userId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID notificationId) {
        notificationRepository.delete(findOwned(userId, notificationId));
    }

    /** Thông báo của người khác trả 404 như không tồn tại. */
    private NotificationEntity findOwned(UUID userId, UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .filter(n -> n.getUserId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy thông báo"));
    }

    private NotificationDTO toDTO(NotificationEntity entity) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(entity.getId());
        dto.setType(entity.getType());
        dto.setTitle(entity.getTitle());
        dto.setBody(entity.getBody());
        dto.setLink(entity.getLink());
        dto.setEntityType(entity.getEntityType());
        dto.setEntityId(entity.getEntityId());
        dto.setIsRead(entity.getIsRead());
        dto.setReadAt(entity.getReadAt());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
