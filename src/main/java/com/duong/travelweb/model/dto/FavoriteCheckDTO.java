package com.duong.travelweb.model.dto;

import java.util.UUID;

public class FavoriteCheckDTO {
    private Boolean favorited;
    private UUID favoriteId;

    public FavoriteCheckDTO() {
    }

    public FavoriteCheckDTO(Boolean favorited, UUID favoriteId) {
        this.favorited = favorited;
        this.favoriteId = favoriteId;
    }

    public Boolean getFavorited() {
        return favorited;
    }

    public void setFavorited(Boolean favorited) {
        this.favorited = favorited;
    }

    public UUID getFavoriteId() {
        return favoriteId;
    }

    public void setFavoriteId(UUID favoriteId) {
        this.favoriteId = favoriteId;
    }
}
