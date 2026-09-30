package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.FavoriteCheckDTO;
import com.duong.travelweb.model.dto.FavoriteDTO;
import com.duong.travelweb.model.dto.FavoriteRequestDTO;
import com.duong.travelweb.service.FavoriteService;
import com.duong.travelweb.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class FavoriteAPI {
    private final FavoriteService favoriteService;

    public FavoriteAPI(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping("/api/favorites/")
    public ResponseEntity<List<FavoriteDTO>> getFavorites(@RequestParam(value = "type", required = false) String type) {
        return ResponseEntity.ok(favoriteService.findMyFavorites(SecurityUtil.getCurrentUserId(), type));
    }

    @GetMapping("/api/favorites/ids/")
    public ResponseEntity<List<FavoriteDTO>> getFavoriteRefs() {
        return ResponseEntity.ok(favoriteService.findMyFavoriteRefs(SecurityUtil.getCurrentUserId()));
    }

    @PostMapping("/api/favorites/")
    public ResponseEntity<FavoriteDTO> addFavorite(@Valid @RequestBody FavoriteRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(favoriteService.addFavorite(SecurityUtil.getCurrentUserId(), request));
    }

    @DeleteMapping("/api/favorites/{favoriteId}/")
    public ResponseEntity<Void> removeFavorite(@PathVariable("favoriteId") UUID favoriteId) {
        favoriteService.removeFavorite(SecurityUtil.getCurrentUserId(), favoriteId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/favorites/check/")
    public ResponseEntity<FavoriteCheckDTO> check(@RequestParam("itemType") String itemType,
                                                  @RequestParam("itemId") UUID itemId) {
        return ResponseEntity.ok(favoriteService.check(SecurityUtil.getCurrentUserId(), itemType, itemId));
    }
}
