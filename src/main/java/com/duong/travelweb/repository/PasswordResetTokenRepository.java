package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

    @Query("SELECT t FROM PasswordResetTokenEntity t JOIN FETCH t.user WHERE t.tokenHash = :tokenHash")
    Optional<PasswordResetTokenEntity> findByTokenHash(@Param("tokenHash") String tokenHash);
}
