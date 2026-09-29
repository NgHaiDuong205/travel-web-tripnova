package com.duong.travelweb.service.impl;

import com.duong.travelweb.model.dto.AuditLogDTO;
import com.duong.travelweb.model.entity.AuditLogEntity;
import com.duong.travelweb.repository.AuditLogRepository;
import com.duong.travelweb.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AuditLogServiceImpl implements AuditLogService {
    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);
    private static final int MAX_USER_AGENT = 500;

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditEntry entry) {
        try {
            AuditLogEntity entity = new AuditLogEntity();
            entity.setUserId(entry.userId());
            entity.setUserEmail(entry.userEmail());
            entity.setAction(entry.action());
            entity.setHttpMethod(entry.httpMethod());
            entity.setPath(entry.path());
            entity.setEntityType(entry.entityType());
            entity.setEntityId(entry.entityId());
            entity.setStatusCode(entry.statusCode());
            entity.setIpAddress(entry.ipAddress());
            String userAgent = entry.userAgent();
            entity.setUserAgent(userAgent != null && userAgent.length() > MAX_USER_AGENT
                    ? userAgent.substring(0, MAX_USER_AGENT) : userAgent);
            entity.setDetails(entry.details() == null || entry.details().isEmpty()
                    ? null : objectMapper.writeValueAsString(entry.details()));
            auditLogRepository.save(entity);
        } catch (RuntimeException e) {
            log.warn("Không ghi được audit log {} {}: {}", entry.httpMethod(), entry.path(), e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDTO> search(UUID userId, String action, LocalDate from, LocalDate to, int page, int limit) {
        Specification<AuditLogEntity> spec = (root, query, cb) -> cb.conjunction();
        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }
        if (action != null && !action.isBlank()) {
            String exact = action.trim();
            spec = spec.and((root, query, cb) -> cb.or(cb.equal(root.get("action"), exact),
                    cb.like(root.get("action"), exact.replace("!", "!!").replace("%", "!%").replace("_", "!_") + ".%", '!')));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
        }
        PageRequest pageable = PageRequest.of(Math.max(page, 1) - 1, limit, Sort.by(Sort.Order.desc("id")));
        return auditLogRepository.findAll(spec, pageable).map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> actions() {
        return auditLogRepository.findDistinctActions();
    }

    private AuditLogDTO toDTO(AuditLogEntity entity) {
        AuditLogDTO dto = new AuditLogDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setUserEmail(entity.getUserEmail());
        dto.setAction(entity.getAction());
        dto.setHttpMethod(entity.getHttpMethod());
        dto.setPath(entity.getPath());
        dto.setEntityType(entity.getEntityType());
        dto.setEntityId(entity.getEntityId());
        dto.setStatusCode(entity.getStatusCode());
        dto.setIpAddress(entity.getIpAddress());
        dto.setDetails(entity.getDetails());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
