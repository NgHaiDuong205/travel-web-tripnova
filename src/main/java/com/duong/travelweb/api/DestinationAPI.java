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

    private final DestinationService destinationService;
    private final LandmarkService landmarkService;

    public DestinationAPI(DestinationService destinationService, LandmarkService landmarkService) {
        this.destinationService = destinationService;
        this.landmarkService = landmarkService;
    }

    @GetMapping("/api/destinations/")
    public ResponseEntity<List<DestinationDTO>> getDestinations(
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String continentCode) {
        List<DestinationDTO> results = destinationService.findDestinations(countryCode, continentCode);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/api/destinations/{destinationId}/")
    public ResponseEntity<DestinationDTO> getDestinationById(@PathVariable("destinationId") UUID destinationId) {
        DestinationDTO result = destinationService.findById(destinationId);
        if (result != null) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/api/destinations/{destinationId}/landmarks/")
    public ResponseEntity<List<LandmarkDTO>> getLandmarksByDestinationId(
            @PathVariable("destinationId") UUID destinationId,
            @RequestParam(required = false) String category) {
        List<LandmarkDTO> results = destinationService.findLandmarksByDestinationId(destinationId, category);
        if (results != null) {
            return ResponseEntity.ok(results);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/api/destinations/{destinationId}/landmarks/{landmarkId}/")
    public ResponseEntity<LandmarkDTO> getLandmarkByIdAndDestinationId(
            @PathVariable("destinationId") UUID destinationId,
            @PathVariable("landmarkId") UUID landmarkId) {
        LandmarkDTO result = landmarkService.findLandmarkByIdAndDestinationId(destinationId, landmarkId);
        if (result != null) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.notFound().build();
    }
}
