package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.DashboardDTO;
import com.duong.travelweb.model.dto.ProfileUpdateRequestDTO;
import com.duong.travelweb.model.dto.UserDTO;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserDTO getProfile(UUID userId);
    UserDTO updateProfile(UUID userId, ProfileUpdateRequestDTO request);
    DashboardDTO getDashboard(UUID userId);

    // ---- Admin ----
    List<UserDTO> findForAdmin(String keyword, String role, Boolean active, int page, int limit);
    long countForAdmin(String keyword, String role, Boolean active);
    UserDTO getUserForAdmin(UUID userId);

    /** Khoá / mở khoá tài khoản. Khoá thì thu hồi mọi refresh token của user. */
    UserDTO updateStatus(UUID adminId, UUID userId, boolean active);

    /** Đặt lại toàn bộ role của user. */
    UserDTO updateRoles(UUID adminId, UUID userId, List<String> roles);

    /**
     * Xoá mềm (deleted_at) + khoá + thu hồi phiên. Giữ nguyên email và lịch sử đơn hàng.
     * 409 nếu user còn đặt phòng chưa kết thúc.
     */
    void deleteByAdmin(UUID adminId, UUID userId);
}
