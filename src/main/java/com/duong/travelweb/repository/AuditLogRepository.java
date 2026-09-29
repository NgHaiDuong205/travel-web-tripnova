package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long>, JpaSpecificationExecutor<AuditLogEntity> {

    /** Danh sách action đã từng ghi (cho bộ lọc ở trang admin). */
    @Query("SELECT DISTINCT a.action FROM AuditLogEntity a ORDER BY a.action")
    List<String> findDistinctActions();
}
