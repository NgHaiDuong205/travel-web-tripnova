package com.duong.travelweb.config;

import com.duong.travelweb.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ghi audit log cho mọi request GHI (POST/PUT/PATCH/DELETE) vào /api/admin/** và /api/account/** — kể cả request lỗi.
 * Không ghi body (có thể chứa mật khẩu); chỉ ghi path variable + query string.
 * action = các đoạn path cố định sau /api/admin/ (hoặc /api/) nối bằng dấu chấm + create|update|delete,
 * VD PUT /api/admin/hotels/{hotelId}/ -> "hotels.update", POST /api/admin/hotel-bookings/{id}/refund/ -> "hotel-bookings.refund.create".
 */
@Component
public class AuditLogInterceptor implements HandlerInterceptor {
    private static final String ADMIN_PREFIX = "/api/admin/";
    private static final String ACCOUNT_PREFIX = "/api/account/";

    private final AuditLogService auditLogService;

    public AuditLogInterceptor(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String method = request.getMethod();
        if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
            return;
        }
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern == null) {
            return;
        }
        String relative;
        if (pattern.startsWith(ADMIN_PREFIX)) {
            relative = pattern.substring(ADMIN_PREFIX.length());
        } else if (pattern.startsWith(ACCOUNT_PREFIX)) {
            relative = pattern.substring("/api/".length());
        } else {
            return;
        }

        List<String> nouns = new ArrayList<>();
        for (String segment : relative.split("/")) {
            if (!segment.isEmpty() && !segment.startsWith("{")) {
                nouns.add(segment);
            }
        }
        String verb = switch (method) {
            case "POST" -> "create";
            case "DELETE" -> "delete";
            default -> "update";
        };
        String action = (nouns.isEmpty() ? "unknown" : String.join(".", nouns)) + "." + verb;

        @SuppressWarnings("unchecked")
        Map<String, String> pathVars = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        String entityId = pathVars == null || pathVars.isEmpty() ? null : pathVars.values().iterator().next();
        Map<String, Object> details = new LinkedHashMap<>();
        if (pathVars != null && !pathVars.isEmpty()) {
            details.put("pathVariables", pathVars);
        }
        if (request.getQueryString() != null) {
            details.put("query", request.getQueryString());
        }
        if (ex != null) {
            details.put("error", ex.getClass().getSimpleName());
        }

        UUID userId = null;
        String email = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            userId = UUID.fromString(jwt.getSubject());
            email = jwt.getClaimAsString("email");
        }
        String uri = request.getRequestURI();
        auditLogService.record(new AuditLogService.AuditEntry(userId, email, action, method, uri,
                nouns.isEmpty() ? null : nouns.get(0), entityId, response.getStatus(), request.getRemoteAddr(),
                request.getHeader("User-Agent"), details));
    }
}
