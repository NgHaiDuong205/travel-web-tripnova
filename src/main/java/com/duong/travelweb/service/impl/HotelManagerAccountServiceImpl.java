package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.HotelAssignmentRequestDTO;
import com.duong.travelweb.model.dto.HotelManagerDTO;
import com.duong.travelweb.model.dto.HotelManagerRequestDTO;
import com.duong.travelweb.model.dto.ManagedHotelDTO;
import com.duong.travelweb.model.entity.HotelEntity;
import com.duong.travelweb.model.entity.RoleEntity;
import com.duong.travelweb.model.entity.UserEntity;
import com.duong.travelweb.model.entity.UserRoleEntity;
import com.duong.travelweb.repository.HotelRepository;
import com.duong.travelweb.repository.RoleRepository;
import com.duong.travelweb.repository.UserRepository;
import com.duong.travelweb.repository.UserRoleRepository;
import com.duong.travelweb.service.HotelManagerAccountService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class HotelManagerAccountServiceImpl implements HotelManagerAccountService {
    public static final String MANAGER_ROLE = "HOTEL_MANAGER";
    /** Bỏ ký tự dễ nhầm (0/O, 1/l/I) vì admin thường đọc / gõ lại mật khẩu tạm cho khách sạn. */
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int GENERATED_PASSWORD_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final HotelRepository hotelRepository;
    private final PasswordEncoder passwordEncoder;

    public HotelManagerAccountServiceImpl(UserRepository userRepository,
                                          RoleRepository roleRepository,
                                          UserRoleRepository userRoleRepository,
                                          HotelRepository hotelRepository,
                                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.hotelRepository = hotelRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelManagerDTO> findManagers() {
        List<UserEntity> managers = userRoleRepository.findActiveUsersByRole(MANAGER_ROLE);
        if (managers.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<ManagedHotelDTO>> hotelsByManager = new HashMap<>();
        for (HotelEntity hotel : hotelRepository.findByManagerIds(managers.stream().map(UserEntity::getId).toList())) {
            hotelsByManager.computeIfAbsent(hotel.getManagedBy().getId(), k -> new ArrayList<>()).add(toHotelDTO(hotel));
        }
        return managers.stream()
                .map(user -> toDTO(user, hotelsByManager.getOrDefault(user.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional
    public HotelManagerDTO createManager(UUID adminId, HotelManagerRequestDTO request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Email đã được sử dụng");
        }
        List<UUID> hotelIds = requireHotels(request.getHotelIds());
        boolean generated = request.getPassword() == null || request.getPassword().isBlank();
        String password = generated ? generatePassword() : request.getPassword();

        LocalDateTime now = LocalDateTime.now();
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone() == null || request.getPhone().isBlank() ? null : request.getPhone().trim());
        user.setLoyaltyPoints(0);
        // Admin tạo và giao tài khoản trực tiếp cho khách sạn → không cần bước xác thực email.
        user.setIsVerified(true);
        user.setIsActive(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user = userRepository.save(user);

        // Chỉ role HOTEL_MANAGER (không kèm USER): tài khoản dùng cho việc quản lý, không đặt chỗ như khách.
        RoleEntity role = roleRepository.findByName(MANAGER_ROLE)
                .orElseThrow(() -> new IllegalStateException("Thiếu role " + MANAGER_ROLE + " trong bảng roles"));
        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setAssignedAt(now);
        userRole.setAssignedBy(adminId);
        userRoleRepository.save(userRole);

        if (!hotelIds.isEmpty()) {
            hotelRepository.assignManager(user.getId(), hotelIds);
        }
        HotelManagerDTO dto = toDTO(user, loadHotels(user.getId()));
        if (generated) {
            dto.setTemporaryPassword(password);
        }
        return dto;
    }

    @Override
    @Transactional
    public HotelManagerDTO assignHotels(UUID managerId, HotelAssignmentRequestDTO request) {
        UserEntity user = userRepository.findById(managerId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy tài khoản"));
        if (!userRoleRepository.hasRole(managerId, MANAGER_ROLE)) {
            throw ApiException.badRequest("Tài khoản này không có quyền quản lý khách sạn (role " + MANAGER_ROLE + ")");
        }
        List<UUID> hotelIds = requireHotels(request.getHotelIds());
        hotelRepository.clearManager(managerId);
        if (!hotelIds.isEmpty()) {
            // Khách sạn đang do manager khác quản lý sẽ chuyển sang manager này (admin chủ động chọn).
            hotelRepository.assignManager(managerId, hotelIds);
        }
        return toDTO(user, loadHotels(managerId));
    }

    /** Bỏ trùng; mọi id phải là khách sạn có thật. */
    private List<UUID> requireHotels(List<UUID> ids) {
        List<UUID> unique = new ArrayList<>(new LinkedHashSet<>(ids));
        unique.remove(null);
        List<UUID> found = hotelRepository.findAllById(unique).stream().map(HotelEntity::getId).toList();
        for (UUID id : unique) {
            if (!found.contains(id)) {
                throw ApiException.notFound("Không tìm thấy khách sạn: " + id);
            }
        }
        return unique;
    }

    private List<ManagedHotelDTO> loadHotels(UUID managerId) {
        // Câu UPDATE native không đi qua persistence context → đọc lại từ DB
        return hotelRepository.findByManagerIds(List.of(managerId)).stream().map(this::toHotelDTO).toList();
    }

    private ManagedHotelDTO toHotelDTO(HotelEntity hotel) {
        return new ManagedHotelDTO(hotel.getId(), hotel.getName(), hotel.getIsActive(),
                hotel.getDestination() == null ? null : hotel.getDestination().getName());
    }

    private HotelManagerDTO toDTO(UserEntity user, List<ManagedHotelDTO> hotels) {
        HotelManagerDTO dto = new HotelManagerDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setIsActive(user.getIsActive());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLastLoginAt(user.getLastLoginAt());
        dto.setHotels(new ArrayList<>(hotels));
        return dto;
    }

    private static String generatePassword() {
        StringBuilder sb = new StringBuilder(GENERATED_PASSWORD_LENGTH);
        for (int i = 0; i < GENERATED_PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
