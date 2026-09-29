package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Giỏ khách sạn (/api/cart) chỉ thấy dòng item_type = hotel; dòng tour / car / flight thuộc giỏ chuyến đi
 * (/api/trip-cart) — cùng bảng nhưng tách truy vấn để trang giỏ hàng cũ không bị ảnh hưởng.
 */
public interface CartItemRepository extends JpaRepository<CartItemEntity, UUID> {

    @Query("SELECT i FROM CartItemEntity i LEFT JOIN FETCH i.roomType rt LEFT JOIN FETCH rt.hotel " +
           "WHERE i.cart.id = :cartId AND i.itemType = 'hotel' ORDER BY i.addedAt")
    List<CartItemEntity> findByCartId(@Param("cartId") UUID cartId);

    @Query("SELECT i FROM CartItemEntity i WHERE i.id = :itemId AND i.cart.id = :cartId AND i.itemType = 'hotel'")
    Optional<CartItemEntity> findInCart(@Param("itemId") UUID itemId, @Param("cartId") UUID cartId);

    @Modifying
    @Query("DELETE FROM CartItemEntity i WHERE i.cart.id = :cartId AND i.itemType = 'hotel'")
    int deleteByCartId(@Param("cartId") UUID cartId);

    @Query("SELECT i FROM CartItemEntity i WHERE i.cart.id = :cartId AND i.itemType IN ('tour', 'car', 'flight') " +
           "ORDER BY i.addedAt")
    List<CartItemEntity> findTripItems(@Param("cartId") UUID cartId);

    @Query("SELECT i FROM CartItemEntity i WHERE i.id = :itemId AND i.cart.id = :cartId " +
           "AND i.itemType IN ('tour', 'car', 'flight')")
    Optional<CartItemEntity> findTripItem(@Param("itemId") UUID itemId, @Param("cartId") UUID cartId);
}
