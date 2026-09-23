package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    @Query("SELECT rt FROM RefreshTokenEntity rt JOIN FETCH rt.user WHERE rt.tokenHash = :tokenHash")
    Optional<RefreshTokenEntity> findByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("UPDATE RefreshTokenEntity rt SET rt.revokedAt = :now WHERE rt.user.id = :userId AND rt.revokedAt IS NULL")
    int revokeAllByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
}
