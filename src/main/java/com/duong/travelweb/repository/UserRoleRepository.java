package com.duong.travelweb.repository;

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
}
