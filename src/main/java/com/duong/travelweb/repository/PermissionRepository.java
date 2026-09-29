package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<PermissionEntity, UUID> {

    @Query("SELECT p FROM PermissionEntity p ORDER BY p.code")
    List<PermissionEntity> findAllOrdered();

    @Query("SELECT COUNT(p) > 0 FROM PermissionEntity p WHERE p.code = :code")
    boolean existsByCode(@Param("code") String code);

    // role_permissions không có entity (PK ghép, không cột id) -> thao tác bằng native query.

    /** [role_id, permission_id] của các role. */
    @Query(value = "SELECT role_id, permission_id FROM role_permissions WHERE role_id IN (:roleIds)", nativeQuery = true)
    List<Object[]> findLinksByRoleIds(@Param("roleIds") List<UUID> roleIds);

    @Modifying
    @Query(value = "DELETE FROM role_permissions WHERE role_id = :roleId", nativeQuery = true)
    int deleteLinksByRoleId(@Param("roleId") UUID roleId);

    @Modifying
    @Query(value = "INSERT INTO role_permissions (role_id, permission_id, assigned_at) VALUES (:roleId, :permissionId, now()) " +
                   "ON CONFLICT DO NOTHING", nativeQuery = true)
    int insertLink(@Param("roleId") UUID roleId, @Param("permissionId") UUID permissionId);
}
