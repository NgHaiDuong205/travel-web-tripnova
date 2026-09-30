package com.duong.travelweb.service.impl;

import com.duong.travelweb.converter.UserDTOConverter;
import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.DashboardDTO;
import com.duong.travelweb.model.dto.HotelBookingDTO;
import com.duong.travelweb.model.dto.ProfileUpdateRequestDTO;
import com.duong.travelweb.model.dto.UserDTO;
import com.duong.travelweb.model.entity.RoleEntity;
import com.duong.travelweb.model.entity.UserEntity;
import com.duong.travelweb.model.entity.UserRoleEntity;
import com.duong.travelweb.repository.HotelBookingRepository;
import com.duong.travelweb.repository.RefreshTokenRepository;
import com.duong.travelweb.repository.RoleRepository;
import com.duong.travelweb.repository.UserRepository;
import com.duong.travelweb.repository.UserRoleRepository;
import com.duong.travelweb.service.HotelBookingService;
import com.duong.travelweb.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    private static final int RECENT_BOOKINGS_LIMIT = 5;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final HotelBookingRepository hotelBookingRepository;
    private final HotelBookingService hotelBookingService;
    private final UserDTOConverter userDTOConverter;
    private final String currencyCode;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           UserRoleRepository userRoleRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           HotelBookingRepository hotelBookingRepository,
                           HotelBookingService hotelBookingService,
                           UserDTOConverter userDTOConverter,
                           @Value("${app.booking.currency:USD}") String currencyCode) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.hotelBookingRepository = hotelBookingRepository;
        this.hotelBookingService = hotelBookingService;
        this.userDTOConverter = userDTOConverter;
        this.currencyCode = currencyCode;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getProfile(UUID userId) {
        UserEntity user = findUser(userId);
        return userDTOConverter.toUserDTO(user, userRepository.findRoleNamesByUserId(userId));
    }

    @Override
    @Transactional
    public UserDTO updateProfile(UUID userId, ProfileUpdateRequestDTO request) {
        UserEntity user = findUser(userId);
        user.setFullName(request.getFullName().trim());
        user.setPhone(blankToNull(request.getPhone()));
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(blankToNull(request.getGender()));
        user.setAvatarUrl(blankToNull(request.getAvatarUrl()));
        user.setUpdatedAt(LocalDateTime.now());
        return userDTOConverter.toUserDTO(user, userRepository.findRoleNamesByUserId(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardDTO getDashboard(UUID userId) {
        UserEntity user = findUser(userId);
        DashboardDTO dto = new DashboardDTO();
        dto.setTotalBookings(hotelBookingService.countMyBookings(userId, "all"));
        dto.setUpcomingBookings(hotelBookingService.countMyBookings(userId, "upcoming"));
        dto.setCompletedBookings(hotelBookingService.countMyBookings(userId, "completed"));
        dto.setCancelledBookings(hotelBookingService.countMyBookings(userId, "cancelled"));
        dto.setTotalSpent(hotelBookingRepository.sumSpentByUser(userId));
        dto.setCurrencyCode(currencyCode);
        dto.setLoyaltyPoints(user.getLoyaltyPoints());
        dto.setRecentBookings(hotelBookingService.findMyBookings(userId, "all", 1, RECENT_BOOKINGS_LIMIT));

        HotelBookingDTO next = hotelBookingService.findNextUpcoming(userId);
        dto.setNextBooking(next);
        if (next != null) {
            dto.setDaysUntilNextTrip(Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), next.getCheckInDate())));
        }
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> findForAdmin(String keyword, String role, Boolean active, int page, int limit) {
        List<UserEntity> users = userRepository.findForAdmin(blankToNull(keyword), blankToNull(role), active, page, limit);
        if (users.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<String>> rolesByUser = new HashMap<>();
        for (Object[] row : userRepository.findRoleNamesByUserIds(users.stream().map(UserEntity::getId).toList())) {
            rolesByUser.computeIfAbsent((UUID) row[0], k -> new ArrayList<>()).add((String) row[1]);
        }
        return users.stream()
                .map(u -> userDTOConverter.toUserDTO(u, rolesByUser.getOrDefault(u.getId(), new ArrayList<>())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countForAdmin(String keyword, String role, Boolean active) {
        return userRepository.countForAdmin(blankToNull(keyword), blankToNull(role), active);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserForAdmin(UUID userId) {
        return getProfile(userId);
    }

    @Override
    @Transactional
    public UserDTO updateStatus(UUID adminId, UUID userId, boolean active) {
        if (adminId.equals(userId) && !active) {
            throw ApiException.badRequest("Không thể tự khoá tài khoản của chính mình");
        }
        UserEntity user = findUser(userId);
        LocalDateTime now = LocalDateTime.now();
        user.setIsActive(active);
        user.setUpdatedAt(now);
        if (!active) {
            refreshTokenRepository.revokeAllByUserId(userId, now);
        }
        return userDTOConverter.toUserDTO(user, userRepository.findRoleNamesByUserId(userId));
    }

    @Override
    @Transactional
    public UserDTO updateRoles(UUID adminId, UUID userId, List<String> roles) {
        UserEntity user = findUser(userId);
        List<String> distinctRoles = roles.stream().map(String::trim).filter(r -> !r.isEmpty()).distinct().toList();
        if (distinctRoles.isEmpty()) {
            throw ApiException.badRequest("Người dùng phải có ít nhất 1 role");
        }
        if (adminId.equals(userId) && !distinctRoles.contains("ADMIN")) {
            throw ApiException.badRequest("Không thể tự gỡ quyền ADMIN của chính mình");
        }
        // Tài khoản khách sạn chỉ do admin tạo ở trang Hotel Managers: không cấp HOTEL_MANAGER cho tài khoản khách
        // và không trộn / gỡ role của tài khoản khách sạn ở đây (khoá hoặc xoá tài khoản thì vẫn làm được).
        String managerRole = HotelManagerAccountServiceImpl.MANAGER_ROLE;
        if (distinctRoles.contains(managerRole) || userRoleRepository.hasRole(userId, managerRole)) {
            throw ApiException.badRequest("Quyền quản lý khách sạn chỉ được cấp khi tạo tài khoản ở trang Hotel Managers;"
                    + " không đổi role của tài khoản khách sạn tại đây");
        }
        List<RoleEntity> roleEntities = distinctRoles.stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> ApiException.badRequest("Role không tồn tại: " + name)))
                .toList();

        LocalDateTime now = LocalDateTime.now();
        userRoleRepository.deleteByUserId(userId);
        for (RoleEntity role : roleEntities) {
            UserRoleEntity userRole = new UserRoleEntity();
            userRole.setUser(user);
            userRole.setRole(role);
            userRole.setAssignedAt(now);
            userRole.setAssignedBy(adminId);
            userRoleRepository.save(userRole);
        }
        // Role nằm trong JWT -> buộc đăng nhập lại để nhận quyền mới.
        refreshTokenRepository.revokeAllByUserId(userId, now);
        return userDTOConverter.toUserDTO(user, distinctRoles);
    }

    @Override
    @Transactional
    public void deleteByAdmin(UUID adminId, UUID userId) {
        if (adminId.equals(userId)) {
            throw ApiException.badRequest("Không thể tự xoá tài khoản của chính mình");
        }
        UserEntity user = findUser(userId);
        if (hotelBookingRepository.existsOpenBookingForUser(userId, LocalDate.now())) {
            throw ApiException.conflict("Người dùng còn đặt phòng chưa kết thúc, hãy huỷ hoặc hoàn tất trước khi xoá");
        }
        LocalDateTime now = LocalDateTime.now();
        user.setDeletedAt(now);
        user.setIsActive(false);
        user.setUpdatedAt(now);
        refreshTokenRepository.revokeAllByUserId(userId, now);
    }

    private UserEntity findUser(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy người dùng"));
        if (user.getDeletedAt() != null) {
            throw ApiException.notFound("Không tìm thấy người dùng");
        }
        return user;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
