package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.UserEntity;
import com.duong.travelweb.repository.custom.UserRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID>, UserRepositoryCustom {

    @Query("SELECT u FROM UserEntity u WHERE LOWER(u.email) = LOWER(:email) AND u.deletedAt IS NULL")
    Optional<UserEntity> findActiveByEmail(@Param("email") String email);

    @Query("SELECT COUNT(u) > 0 FROM UserEntity u WHERE LOWER(u.email) = LOWER(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);

    @Query("SELECT ur.role.name FROM UserRoleEntity ur WHERE ur.user.id = :userId")
    List<String> findRoleNamesByUserId(@Param("userId") UUID userId);

    /** [userId, roleName] theo lô. */
    @Query("SELECT ur.user.id, ur.role.name FROM UserRoleEntity ur WHERE ur.user.id IN :userIds")
    List<Object[]> findRoleNamesByUserIds(@Param("userIds") List<UUID> userIds);

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.deletedAt IS NULL")
    long countActiveAccounts();

    /** Số tài khoản đăng ký trong [from, to) (kể cả đã xoá sau đó). */
    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.createdAt >= :from AND u.createdAt < :to")
    long countCreatedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** [ngày đầu kỳ, số user mới]; unit = day | week | month. */
    @Query(value = "SELECT CAST(date_trunc(CAST(:unit AS text), created_at) AS date) AS bucket, COUNT(*) FROM users " +
                   "WHERE created_at >= :from AND created_at < :to GROUP BY 1 ORDER BY 1",
           nativeQuery = true)
    List<Object[]> countCreatedByBucket(@Param("unit") String unit, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
