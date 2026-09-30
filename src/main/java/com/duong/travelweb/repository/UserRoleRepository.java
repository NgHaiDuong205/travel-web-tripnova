package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.UserEntity;
import com.duong.travelweb.model.entity.UserRoleEntity;
import com.duong.travelweb.model.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleId> {

    @Modifying
    @Query("DELETE FROM UserRoleEntity ur WHERE ur.user.id = :userId")
    int deleteByUserId(@Param("userId") UUID userId);

    /** [role_id, số user chưa bị xoá đang giữ role]. */
    @Query("SELECT ur.role.id, COUNT(ur) FROM UserRoleEntity ur WHERE ur.user.deletedAt IS NULL GROUP BY ur.role.id")
    List<Object[]> countActiveUsersGroupByRole();

    /** Người dùng (chưa xoá) có role, mới tạo trước. */
    @Query("SELECT ur.user FROM UserRoleEntity ur WHERE ur.role.name = :role AND ur.user.deletedAt IS NULL ORDER BY ur.user.createdAt DESC")
    List<UserEntity> findActiveUsersByRole(@Param("role") String role);

    @Query("SELECT COUNT(ur) > 0 FROM UserRoleEntity ur WHERE ur.user.id = :userId AND ur.role.name = :role")
    boolean hasRole(@Param("userId") UUID userId, @Param("role") String role);
}
