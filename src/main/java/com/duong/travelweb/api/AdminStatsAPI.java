package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.AdminStatsDTO;
import com.duong.travelweb.service.AdminStatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class AdminStatsAPI {
    private final AdminStatsService adminStatsService;

    public AdminStatsAPI(AdminStatsService adminStatsService) {
        this.adminStatsService = adminStatsService;
    }

    /** Thống kê cho trang admin (ROLE_ADMIN qua /api/admin/**). from/to tính cả 2 đầu, mặc định 30 ngày gần nhất; granularity = day | week | month. */
    @GetMapping("/api/admin/stats/{section}/")
    public ResponseEntity<AdminStatsDTO> getStats(
            @PathVariable("section") String section,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "granularity", required = false) String granularity) {
        return ResponseEntity.ok(adminStatsService.getStats(section, from, to, granularity));
    }
}
