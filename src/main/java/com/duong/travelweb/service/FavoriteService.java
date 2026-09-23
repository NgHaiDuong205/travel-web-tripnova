package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.FavoriteCheckDTO;
import com.duong.travelweb.model.dto.FavoriteDTO;
import com.duong.travelweb.model.dto.FavoriteRequestDTO;

import java.util.List;
import java.util.UUID;

public interface FavoriteService {
    List<FavoriteDTO> findMyFavorites(UUID userId, String itemType);

    /** Thêm vào yêu thích; đã có thì trả về bản ghi cũ (idempotent). */
    FavoriteDTO addFavorite(UUID userId, FavoriteRequestDTO request);

    void removeFavorite(UUID userId, UUID favoriteId);

    FavoriteCheckDTO check(UUID userId, String itemType, UUID itemId);
}
