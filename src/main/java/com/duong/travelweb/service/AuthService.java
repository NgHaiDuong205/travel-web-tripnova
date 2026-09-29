package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.AuthResponseDTO;
import com.duong.travelweb.model.dto.ChangePasswordRequestDTO;
import com.duong.travelweb.model.dto.LoginRequestDTO;
import com.duong.travelweb.model.dto.RegisterRequestDTO;
import com.duong.travelweb.model.dto.ResetPasswordRequestDTO;
import com.duong.travelweb.model.dto.UserDTO;

import java.util.UUID;

public interface AuthService {
    AuthResponseDTO register(RegisterRequestDTO request, String deviceInfo, String ipAddress);
    AuthResponseDTO login(LoginRequestDTO request, String deviceInfo, String ipAddress);
    AuthResponseDTO refreshToken(String refreshToken, String deviceInfo, String ipAddress);
    void logout(String refreshToken);
    UserDTO getCurrentUser(UUID userId);
    void forgotPassword(String email, String ipAddress);
    void resetPassword(ResetPasswordRequestDTO request);
    void changePassword(UUID userId, ChangePasswordRequestDTO request);

    /** Tạo user mới (role USER) từ tài khoản mạng xã hội; email phải chưa tồn tại. */
    UUID registerOAuthUser(String email, String fullName, String avatarUrl, boolean emailVerified);

    /** Cấp access + refresh token cho user đã xác định danh tính bằng cách khác (VD OAuth2). */
    AuthResponseDTO issueTokensForUser(UUID userId, String deviceInfo, String ipAddress);
}
