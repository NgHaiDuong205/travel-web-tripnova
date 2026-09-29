package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Body gán danh sách quyền cho role (thay thế toàn bộ). */
public class PermissionIdsRequestDTO {
    @NotNull(message = "Thiếu danh sách quyền")
    private List<UUID> permissionIds = new ArrayList<>();

    public List<UUID> getPermissionIds() {
        return permissionIds;
    }

    public void setPermissionIds(List<UUID> permissionIds) {
        this.permissionIds = permissionIds;
    }
}
