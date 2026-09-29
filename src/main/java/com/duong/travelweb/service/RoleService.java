package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.PermissionDTO;
import com.duong.travelweb.model.dto.RoleDTO;
import com.duong.travelweb.model.dto.RoleRequestDTO;

import java.util.List;
import java.util.UUID;

/**
 * Quản lý role / permission cho admin. Phân quyền API hiện vẫn dựa trên role hệ thống (USER, ADMIN, HOTEL_MANAGER);
 * permission chỉ là danh mục gán cho role, chưa được kiểm tra khi gọi API.
 */
public interface RoleService {
    List<String> SYSTEM_ROLES = List.of("USER", "ADMIN", "HOTEL_MANAGER");

    List<RoleDTO> findAll();

    RoleDTO create(RoleRequestDTO request);

    /** Role hệ thống chỉ được sửa mô tả, không đổi tên. */
    RoleDTO update(UUID roleId, RoleRequestDTO request);

    /** 400 với role hệ thống, 409 nếu role đang được gán cho user. */
    void delete(UUID roleId);

    List<PermissionDTO> findPermissions();

    /** Thay thế toàn bộ quyền của role. */
    RoleDTO updatePermissions(UUID roleId, List<UUID> permissionIds);
}
