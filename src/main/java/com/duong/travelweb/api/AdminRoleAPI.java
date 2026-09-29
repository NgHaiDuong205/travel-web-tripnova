package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.PermissionDTO;
import com.duong.travelweb.model.dto.PermissionIdsRequestDTO;
import com.duong.travelweb.model.dto.RoleDTO;
import com.duong.travelweb.model.dto.RoleRequestDTO;
import com.duong.travelweb.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Quản lý role / permission. /api/admin/** yêu cầu ROLE_ADMIN (SecurityConfig). */
@RestController
public class AdminRoleAPI {
    private final RoleService roleService;

    public AdminRoleAPI(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/api/admin/roles/")
    public ResponseEntity<List<RoleDTO>> getRoles() {
        return ResponseEntity.ok(roleService.findAll());
    }

    @PostMapping("/api/admin/roles/")
    public ResponseEntity<RoleDTO> createRole(@Valid @RequestBody RoleRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.create(request));
    }

    @PutMapping("/api/admin/roles/{roleId}/")
    public ResponseEntity<RoleDTO> updateRole(@PathVariable("roleId") UUID roleId,
                                              @Valid @RequestBody RoleRequestDTO request) {
        return ResponseEntity.ok(roleService.update(roleId, request));
    }

    @DeleteMapping("/api/admin/roles/{roleId}/")
    public ResponseEntity<Void> deleteRole(@PathVariable("roleId") UUID roleId) {
        roleService.delete(roleId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/admin/roles/{roleId}/permissions/")
    public ResponseEntity<RoleDTO> updateRolePermissions(@PathVariable("roleId") UUID roleId,
                                                         @Valid @RequestBody PermissionIdsRequestDTO request) {
        return ResponseEntity.ok(roleService.updatePermissions(roleId, request.getPermissionIds()));
    }

    @GetMapping("/api/admin/permissions/")
    public ResponseEntity<List<PermissionDTO>> getPermissions() {
        return ResponseEntity.ok(roleService.findPermissions());
    }
}
