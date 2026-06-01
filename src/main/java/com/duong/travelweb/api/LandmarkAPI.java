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

    private final LandmarkService landmarkService;

    public LandmarkAPI(LandmarkService landmarkService) {
        this.landmarkService = landmarkService;
    }

    @GetMapping("/api/landmarks/")
    public ResponseEntity<List<LandmarkDTO>> getAllLandmarks(
            @RequestParam(required = false) String category) {
        List<LandmarkDTO> results = landmarkService.findAllActiveLandmarks(category);
        return ResponseEntity.ok(results);
    }
}
