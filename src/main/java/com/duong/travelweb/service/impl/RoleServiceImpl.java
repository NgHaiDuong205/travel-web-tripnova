package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.PermissionDTO;
import com.duong.travelweb.model.dto.RoleDTO;
import com.duong.travelweb.model.dto.RoleRequestDTO;
import com.duong.travelweb.model.entity.PermissionEntity;
import com.duong.travelweb.model.entity.RoleEntity;
import com.duong.travelweb.repository.PermissionRepository;
import com.duong.travelweb.repository.RoleRepository;
import com.duong.travelweb.repository.UserRoleRepository;
import com.duong.travelweb.service.RoleService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;

    public RoleServiceImpl(RoleRepository roleRepository,
                           PermissionRepository permissionRepository,
                           UserRoleRepository userRoleRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleDTO> findAll() {
        return toDTOs(roleRepository.findAll(Sort.by("createdAt", "name")));
    }

    @Override
    @Transactional
    public RoleDTO create(RoleRequestDTO request) {
        String name = request.getName().trim();
        if (roleRepository.findByName(name).isPresent()) {
            throw ApiException.conflict("Role " + name + " đã tồn tại");
        }
        RoleEntity role = new RoleEntity();
        role.setName(name);
        role.setDescription(blankToNull(request.getDescription()));
        role.setCreatedAt(LocalDateTime.now());
        return toDTOs(List.of(roleRepository.save(role))).get(0);
    }

    @Override
    @Transactional
    public RoleDTO update(UUID roleId, RoleRequestDTO request) {
        RoleEntity role = findRole(roleId);
        String name = request.getName().trim();
        if (!name.equals(role.getName())) {
            if (isSystem(role)) {
                throw ApiException.badRequest("Không thể đổi tên role hệ thống " + role.getName());
            }
            if (roleRepository.findByName(name).isPresent()) {
                throw ApiException.conflict("Role " + name + " đã tồn tại");
            }
            role.setName(name);
        }
        role.setDescription(blankToNull(request.getDescription()));
        return toDTOs(List.of(role)).get(0);
    }

    @Override
    @Transactional
    public void delete(UUID roleId) {
        RoleEntity role = findRole(roleId);
        if (isSystem(role)) {
            throw ApiException.badRequest("Không thể xoá role hệ thống " + role.getName());
        }
        long users = activeUserCounts().getOrDefault(roleId, 0L);
        if (users > 0) {
            throw ApiException.conflict("Role đang được gán cho " + users + " người dùng, hãy gỡ role trước khi xoá");
        }
        // user_roles của user đã xoá mềm và role_permissions: ON DELETE CASCADE
        roleRepository.delete(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionDTO> findPermissions() {
        return permissionRepository.findAllOrdered().stream().map(this::toPermissionDTO).toList();
    }

    @Override
    @Transactional
    public RoleDTO updatePermissions(UUID roleId, List<UUID> permissionIds) {
        RoleEntity role = findRole(roleId);
        List<UUID> ids = permissionIds.stream().distinct().toList();
        if (permissionRepository.findAllById(ids).size() != ids.size()) {
            throw ApiException.badRequest("Có quyền không tồn tại trong danh sách");
        }
        permissionRepository.deleteLinksByRoleId(roleId);
        for (UUID permissionId : ids) {
            permissionRepository.insertLink(roleId, permissionId);
        }
        return toDTOs(List.of(role)).get(0);
    }

    private List<RoleDTO> toDTOs(List<RoleEntity> roles) {
        if (roles.isEmpty()) {
            return List.of();
        }
        Map<UUID, Long> userCounts = activeUserCounts();
        Map<UUID, List<UUID>> permissionsByRole = new HashMap<>();
        for (Object[] row : permissionRepository.findLinksByRoleIds(roles.stream().map(RoleEntity::getId).toList())) {
            permissionsByRole.computeIfAbsent((UUID) row[0], k -> new ArrayList<>()).add((UUID) row[1]);
        }
        return roles.stream().map(role -> {
            RoleDTO dto = new RoleDTO();
            dto.setId(role.getId());
            dto.setName(role.getName());
            dto.setDescription(role.getDescription());
            dto.setSystem(isSystem(role));
            dto.setUserCount(userCounts.getOrDefault(role.getId(), 0L));
            dto.setPermissionIds(permissionsByRole.getOrDefault(role.getId(), new ArrayList<>()));
            dto.setCreatedAt(role.getCreatedAt());
            return dto;
        }).toList();
    }

    private Map<UUID, Long> activeUserCounts() {
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : userRoleRepository.countActiveUsersGroupByRole()) {
            counts.put((UUID) row[0], (Long) row[1]);
        }
        return counts;
    }

    private PermissionDTO toPermissionDTO(PermissionEntity entity) {
        PermissionDTO dto = new PermissionDTO();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        return dto;
    }

    private RoleEntity findRole(UUID roleId) {
        return roleRepository.findById(roleId).orElseThrow(() -> ApiException.notFound("Không tìm thấy role"));
    }

    private boolean isSystem(RoleEntity role) {
        return SYSTEM_ROLES.contains(role.getName());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
