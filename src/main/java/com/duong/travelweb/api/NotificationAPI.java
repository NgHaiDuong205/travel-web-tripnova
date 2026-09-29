package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.NotificationDTO;
import com.duong.travelweb.service.NotificationService;
import com.duong.travelweb.util.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Thông báo trong ứng dụng của user đang đăng nhập. */
@RestController
public class NotificationAPI {
    private static final int MAX_LIMIT = 50;

    private final NotificationService notificationService;

    public NotificationAPI(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** Mới nhất trước; X-Total-Count = tổng theo bộ lọc, X-Unread-Count = số chưa đọc. */
    @GetMapping("/api/me/notifications/")
    public ResponseEntity<List<NotificationDTO>> getMyNotifications(
            @RequestParam(value = "unread", defaultValue = "false") boolean unread,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        UUID userId = SecurityUtil.getCurrentUserId();
        Page<NotificationDTO> result = notificationService.findMine(userId, unread, page, Math.min(Math.max(limit, 1), MAX_LIMIT));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .header("X-Unread-Count", String.valueOf(notificationService.countUnread(userId)))
                .body(result.getContent());
    }

    @GetMapping("/api/me/notifications/unread-count/")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of("count", notificationService.countUnread(SecurityUtil.getCurrentUserId())));
    }

    @PatchMapping("/api/me/notifications/{notificationId}/read/")
    public ResponseEntity<NotificationDTO> markRead(@PathVariable("notificationId") UUID notificationId) {
        return ResponseEntity.ok(notificationService.markRead(SecurityUtil.getCurrentUserId(), notificationId));
    }

    @PostMapping("/api/me/notifications/mark-all-read/")
    public ResponseEntity<Map<String, Integer>> markAllRead() {
        return ResponseEntity.ok(Map.of("updated", notificationService.markAllRead(SecurityUtil.getCurrentUserId())));
    }

    @DeleteMapping("/api/me/notifications/{notificationId}/")
    public ResponseEntity<Void> delete(@PathVariable("notificationId") UUID notificationId) {
        notificationService.delete(SecurityUtil.getCurrentUserId(), notificationId);
        return ResponseEntity.noContent().build();
    }
}
