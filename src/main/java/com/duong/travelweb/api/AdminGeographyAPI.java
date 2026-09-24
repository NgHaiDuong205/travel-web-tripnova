package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.AdminContinentDTO;
import com.duong.travelweb.model.dto.AdminCountryDTO;
import com.duong.travelweb.model.dto.AdminDestinationDTO;
import com.duong.travelweb.model.dto.AdminLandmarkDTO;
import com.duong.travelweb.model.dto.ContinentRequestDTO;
import com.duong.travelweb.model.dto.CountryRequestDTO;
import com.duong.travelweb.model.dto.DestinationRequestDTO;
import com.duong.travelweb.model.dto.LandmarkRequestDTO;
import com.duong.travelweb.service.AdminGeographyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** API quản trị châu lục / quốc gia / điểm đến / địa danh. /api/admin/** yêu cầu ROLE_ADMIN (SecurityConfig). */
@RestController
public class AdminGeographyAPI {
    private static final int MAX_LIMIT = 100;

    private final AdminGeographyService adminGeographyService;

    public AdminGeographyAPI(AdminGeographyService adminGeographyService) {
        this.adminGeographyService = adminGeographyService;
    }

    // ---- Continents ----

    @GetMapping("/api/admin/continents/")
    public ResponseEntity<List<AdminContinentDTO>> getContinents() {
        return ResponseEntity.ok(adminGeographyService.findContinents());
    }

    @PostMapping("/api/admin/continents/")
    public ResponseEntity<AdminContinentDTO> createContinent(@Valid @RequestBody ContinentRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminGeographyService.createContinent(request));
    }

    @PutMapping("/api/admin/continents/{continentId}/")
    public ResponseEntity<AdminContinentDTO> updateContinent(@PathVariable("continentId") UUID continentId,
                                                             @Valid @RequestBody ContinentRequestDTO request) {
        return ResponseEntity.ok(adminGeographyService.updateContinent(continentId, request));
    }

    @DeleteMapping("/api/admin/continents/{continentId}/")
    public ResponseEntity<Void> deleteContinent(@PathVariable("continentId") UUID continentId) {
        adminGeographyService.deleteContinent(continentId);
        return ResponseEntity.noContent().build();
    }

    // ---- Countries ----

    @GetMapping("/api/admin/countries/")
    public ResponseEntity<List<AdminCountryDTO>> getCountries(@RequestParam(value = "q", required = false) String keyword,
                                                              @RequestParam(value = "continentId", required = false) UUID continentId) {
        List<AdminCountryDTO> results = adminGeographyService.findCountries(keyword, continentId);
        return ResponseEntity.ok().header("X-Total-Count", String.valueOf(results.size())).body(results);
    }

    @PostMapping("/api/admin/countries/")
    public ResponseEntity<AdminCountryDTO> createCountry(@Valid @RequestBody CountryRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminGeographyService.createCountry(request));
    }

    @PutMapping("/api/admin/countries/{countryId}/")
    public ResponseEntity<AdminCountryDTO> updateCountry(@PathVariable("countryId") UUID countryId,
                                                         @Valid @RequestBody CountryRequestDTO request) {
        return ResponseEntity.ok(adminGeographyService.updateCountry(countryId, request));
    }

    @DeleteMapping("/api/admin/countries/{countryId}/")
    public ResponseEntity<Void> deleteCountry(@PathVariable("countryId") UUID countryId) {
        adminGeographyService.deleteCountry(countryId);
        return ResponseEntity.noContent().build();
    }

    // ---- Destinations ----

    @GetMapping("/api/admin/destinations/")
    public ResponseEntity<List<AdminDestinationDTO>> getDestinations(@RequestParam(value = "q", required = false) String keyword,
                                                                     @RequestParam(value = "countryId", required = false) UUID countryId,
                                                                     @RequestParam(value = "status", required = false) String status,
                                                                     @RequestParam(value = "page", defaultValue = "1") int page,
                                                                     @RequestParam(value = "limit", defaultValue = "20") int limit) {
        Boolean active = parseActive(status);
        List<AdminDestinationDTO> results = adminGeographyService.findDestinations(keyword, countryId, active, page, clamp(limit));
        long total = adminGeographyService.countDestinations(keyword, countryId, active);
        return ResponseEntity.ok().header("X-Total-Count", String.valueOf(total)).body(results);
    }

    @GetMapping("/api/admin/destinations/{destinationId}/")
    public ResponseEntity<AdminDestinationDTO> getDestination(@PathVariable("destinationId") UUID destinationId) {
        return ResponseEntity.ok(adminGeographyService.getDestination(destinationId));
    }

    @PostMapping("/api/admin/destinations/")
    public ResponseEntity<AdminDestinationDTO> createDestination(@Valid @RequestBody DestinationRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminGeographyService.createDestination(request));
    }

    @PutMapping("/api/admin/destinations/{destinationId}/")
    public ResponseEntity<AdminDestinationDTO> updateDestination(@PathVariable("destinationId") UUID destinationId,
                                                                 @Valid @RequestBody DestinationRequestDTO request) {
        return ResponseEntity.ok(adminGeographyService.updateDestination(destinationId, request));
    }

    @DeleteMapping("/api/admin/destinations/{destinationId}/")
    public ResponseEntity<Void> deleteDestination(@PathVariable("destinationId") UUID destinationId) {
        adminGeographyService.deactivateDestination(destinationId);
        return ResponseEntity.noContent().build();
    }

    // ---- Landmarks ----

    @GetMapping("/api/admin/landmarks/")
    public ResponseEntity<List<AdminLandmarkDTO>> getLandmarks(@RequestParam(value = "q", required = false) String keyword,
                                                               @RequestParam(value = "destinationId", required = false) UUID destinationId,
                                                               @RequestParam(value = "category", required = false) String category,
                                                               @RequestParam(value = "status", required = false) String status,
                                                               @RequestParam(value = "page", defaultValue = "1") int page,
                                                               @RequestParam(value = "limit", defaultValue = "20") int limit) {
        String categoryFilter = category == null || "all".equals(category) ? null : category;
        Boolean active = parseActive(status);
        List<AdminLandmarkDTO> results = adminGeographyService.findLandmarks(keyword, destinationId, categoryFilter, active, page, clamp(limit));
        long total = adminGeographyService.countLandmarks(keyword, destinationId, categoryFilter, active);
        return ResponseEntity.ok().header("X-Total-Count", String.valueOf(total)).body(results);
    }

    @PostMapping("/api/admin/landmarks/")
    public ResponseEntity<AdminLandmarkDTO> createLandmark(@Valid @RequestBody LandmarkRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminGeographyService.createLandmark(request));
    }

    @PutMapping("/api/admin/landmarks/{landmarkId}/")
    public ResponseEntity<AdminLandmarkDTO> updateLandmark(@PathVariable("landmarkId") UUID landmarkId,
                                                           @Valid @RequestBody LandmarkRequestDTO request) {
        return ResponseEntity.ok(adminGeographyService.updateLandmark(landmarkId, request));
    }

    @DeleteMapping("/api/admin/landmarks/{landmarkId}/")
    public ResponseEntity<Void> deleteLandmark(@PathVariable("landmarkId") UUID landmarkId) {
        adminGeographyService.deactivateLandmark(landmarkId);
        return ResponseEntity.noContent().build();
    }

    private Boolean parseActive(String status) {
        return switch (status == null ? "" : status) {
            case "active" -> true;
            case "inactive" -> false;
            default -> null;
        };
    }

    private int clamp(int limit) {
        return limit < 1 ? 20 : Math.min(limit, MAX_LIMIT);
    }
}
