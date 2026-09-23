package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.FavoriteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<FavoriteEntity, UUID> {

    @Query("SELECT f FROM FavoriteEntity f WHERE f.userId = :userId ORDER BY f.createdAt DESC")
    List<FavoriteEntity> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT f FROM FavoriteEntity f WHERE f.userId = :userId AND f.itemType = :itemType ORDER BY f.createdAt DESC")
    List<FavoriteEntity> findByUserIdAndItemType(@Param("userId") UUID userId, @Param("itemType") String itemType);

    @Query("SELECT f FROM FavoriteEntity f WHERE f.userId = :userId AND f.itemType = :itemType AND f.itemId = :itemId")
    Optional<FavoriteEntity> findOne(@Param("userId") UUID userId,
                                     @Param("itemType") String itemType,
                                     @Param("itemId") UUID itemId);
}
