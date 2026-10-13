package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long>, JpaSpecificationExecutor<AuditLogEntity> {

    /** Danh sách action đã từng ghi (cho bộ lọc ở trang admin). */
    @Query("SELECT DISTINCT a.action FROM AuditLogEntity a ORDER BY a.action")
    List<String> findDistinctActions();

    long countByUserIdAndActionAndCreatedAtAfter(UUID userId, String action, LocalDateTime since);
}
