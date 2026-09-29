package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.LandmarkDTO;
import com.duong.travelweb.service.LandmarkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class LandmarkAPI {
    private static final int MAX_LIMIT = 100;

    private final LandmarkService landmarkService;

    public LandmarkAPI(LandmarkService landmarkService) {
        this.landmarkService = landmarkService;
    }

    @GetMapping("/api/landmarks/")
    public ResponseEntity<List<LandmarkDTO>> getAllLandmarks(
            @RequestParam(value = "q", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", required = false) Integer limit) {
        Integer pageSize = limit == null ? null : Math.min(Math.max(limit, 1), MAX_LIMIT);
        List<LandmarkDTO> results = landmarkService.findAllActiveLandmarks(keyword, category, page, pageSize);
        long total = landmarkService.countActiveLandmarks(keyword, category);
        return ResponseEntity.ok().header("X-Total-Count", String.valueOf(total)).body(results);
    }
}
