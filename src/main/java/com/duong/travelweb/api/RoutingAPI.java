package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.RouteDTO;
import com.duong.travelweb.model.dto.RouteRequestDTO;
import com.duong.travelweb.service.RoutingService;
import com.duong.travelweb.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Đường đi thật (OSRM) cho bản đồ lịch trình — đăng nhập. */
@RestController
public class RoutingAPI {
    private final RoutingService routingService;

    public RoutingAPI(RoutingService routingService) {
        this.routingService = routingService;
    }

    /** Body {points: [[lat, lng], …]} (2–25 điểm, theo thứ tự đi). Không lấy được đường → 200 với source "none". */
    @PostMapping("/api/routing/route/")
    public ResponseEntity<RouteDTO> route(@Valid @RequestBody RouteRequestDTO request) {
        return ResponseEntity.ok(routingService.directions(SecurityUtil.getCurrentUserId(), request));
    }
}
