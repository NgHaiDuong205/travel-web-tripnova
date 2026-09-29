package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.CartEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<CartEntity, UUID> {

    @Query("SELECT c FROM CartEntity c WHERE c.userId = :userId")
    Optional<CartEntity> findByUserId(@Param("userId") UUID userId);
}
