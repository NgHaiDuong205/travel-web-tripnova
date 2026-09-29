package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItemEntity, UUID> {

    @Query("SELECT i FROM CartItemEntity i LEFT JOIN FETCH i.roomType rt LEFT JOIN FETCH rt.hotel " +
           "WHERE i.cart.id = :cartId ORDER BY i.addedAt")
    List<CartItemEntity> findByCartId(@Param("cartId") UUID cartId);

    @Query("SELECT i FROM CartItemEntity i WHERE i.id = :itemId AND i.cart.id = :cartId")
    Optional<CartItemEntity> findInCart(@Param("itemId") UUID itemId, @Param("cartId") UUID cartId);

    @Modifying
    @Query("DELETE FROM CartItemEntity i WHERE i.cart.id = :cartId")
    int deleteByCartId(@Param("cartId") UUID cartId);
}
