package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.CartEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<CartEntity, UUID> {

    @Query("SELECT c FROM CartEntity c WHERE c.userId = :userId")
    Optional<CartEntity> findByUserId(@Param("userId") UUID userId);

    /** Giỏ khách theo SHA-256 của token. */
    @Query("SELECT c FROM CartEntity c WHERE c.userId IS NULL AND c.sessionToken = :tokenHash")
    Optional<CartEntity> findGuestCart(@Param("tokenHash") String tokenHash);

    /** Dòng trong giỏ bị xoá theo (FK cart_items.cart_id ON DELETE CASCADE). */
    @Modifying
    @Query("DELETE FROM CartEntity c WHERE c.userId IS NULL AND c.updatedAt < :before")
    int deleteGuestCartsUpdatedBefore(@Param("before") LocalDateTime before);
}
