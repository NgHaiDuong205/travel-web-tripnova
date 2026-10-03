package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.DestinationDTO;
import com.duong.travelweb.model.dto.LandmarkDTO;
import com.duong.travelweb.service.DestinationService;
import com.duong.travelweb.service.LandmarkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class DestinationAPI {
    private static final int MAX_LIMIT = 100;

    private final DestinationService destinationService;
    private final LandmarkService landmarkService;

    public DestinationAPI(DestinationService destinationService, LandmarkService landmarkService) {
        this.destinationService = destinationService;
        this.landmarkService = landmarkService;
    }

    @GetMapping("/api/destinations/")
    public ResponseEntity<List<DestinationDTO>> getDestinations(
            @RequestParam(value = "q", required = false) String keyword,
            @RequestParam(value = "countryCode", required = false) String countryCode,
            @RequestParam(value = "continentCode", required = false) String continentCode,
            @RequestParam(value = "isPopular", required = false) Boolean popular,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", required = false) Integer limit) {
        // Không truyền limit -> trả toàn bộ (dropdown FE); luôn kèm X-Total-Count
        Integer pageSize = limit == null ? null : Math.min(Math.max(limit, 1), MAX_LIMIT);
        List<DestinationDTO> results = destinationService.findDestinations(keyword, countryCode, continentCode, popular, category, page, pageSize);
        long total = destinationService.countDestinations(keyword, countryCode, continentCode, popular, category);
        return ResponseEntity.ok().header("X-Total-Count", String.valueOf(total)).body(results);
    }

    @GetMapping("/api/destinations/{destinationId}/")
    public ResponseEntity<DestinationDTO> getDestinationById(@PathVariable("destinationId") UUID destinationId) {
        return ResponseEntity.ok(destinationService.findById(destinationId));
    }

    @GetMapping("/api/destinations/{destinationId}/landmarks/")
    public ResponseEntity<List<LandmarkDTO>> getLandmarksByDestinationId(
            @PathVariable("destinationId") UUID destinationId,
            @RequestParam(value = "category", required = false) String category) {
        return ResponseEntity.ok(destinationService.findLandmarksByDestinationId(destinationId, category));
    }

    @GetMapping("/api/destinations/{destinationId}/landmarks/{landmarkId}/")
    public ResponseEntity<LandmarkDTO> getLandmarkByIdAndDestinationId(
            @PathVariable("destinationId") UUID destinationId,
            @PathVariable("landmarkId") UUID landmarkId) {
        return ResponseEntity.ok(landmarkService.findLandmarkByIdAndDestinationId(destinationId, landmarkId));
    }
}
