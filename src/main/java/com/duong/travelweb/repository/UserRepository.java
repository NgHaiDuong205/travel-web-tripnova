package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    @Query("SELECT u FROM UserEntity u WHERE LOWER(u.email) = LOWER(:email) AND u.deletedAt IS NULL")
    Optional<UserEntity> findActiveByEmail(@Param("email") String email);

    @Query("SELECT COUNT(u) > 0 FROM UserEntity u WHERE LOWER(u.email) = LOWER(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);

    @Query("SELECT ur.role.name FROM UserRoleEntity ur WHERE ur.user.id = :userId")
    List<String> findRoleNamesByUserId(@Param("userId") UUID userId);
}
