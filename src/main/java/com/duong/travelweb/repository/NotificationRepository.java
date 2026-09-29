package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    @Query(value = "SELECT n FROM NotificationEntity n WHERE n.userId = :userId ORDER BY n.createdAt DESC, n.id",
            countQuery = "SELECT COUNT(n) FROM NotificationEntity n WHERE n.userId = :userId")
    Page<NotificationEntity> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query(value = "SELECT n FROM NotificationEntity n WHERE n.userId = :userId AND n.isRead = false "
            + "ORDER BY n.createdAt DESC, n.id",
            countQuery = "SELECT COUNT(n) FROM NotificationEntity n WHERE n.userId = :userId AND n.isRead = false")
    Page<NotificationEntity> findUnreadByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.userId = :userId AND n.isRead = false")
    long countUnread(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true, n.readAt = :now WHERE n.userId = :userId AND n.isRead = false")
    int markAllRead(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
}
