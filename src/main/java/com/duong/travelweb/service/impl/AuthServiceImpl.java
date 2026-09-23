package com.duong.travelweb.service.impl;

import com.duong.travelweb.converter.UserDTOConverter;
import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.AuthResponseDTO;
import com.duong.travelweb.model.dto.ChangePasswordRequestDTO;
import com.duong.travelweb.model.dto.LoginRequestDTO;
import com.duong.travelweb.model.dto.RegisterRequestDTO;
import com.duong.travelweb.model.dto.ResetPasswordRequestDTO;
import com.duong.travelweb.model.dto.UserDTO;
import com.duong.travelweb.model.entity.PasswordResetTokenEntity;
import com.duong.travelweb.model.entity.RefreshTokenEntity;
import com.duong.travelweb.model.entity.RoleEntity;
import com.duong.travelweb.model.entity.UserEntity;
import com.duong.travelweb.model.entity.UserRoleEntity;
import com.duong.travelweb.repository.PasswordResetTokenRepository;
import com.duong.travelweb.repository.RefreshTokenRepository;
import com.duong.travelweb.repository.RoleRepository;
import com.duong.travelweb.repository.UserRepository;
import com.duong.travelweb.repository.UserRoleRepository;
import com.duong.travelweb.security.JwtService;
import com.duong.travelweb.service.AuthService;
import com.duong.travelweb.util.TokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {
    public static final String DEFAULT_ROLE = "USER";
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserDTOConverter userDTOConverter;
    private final long refreshTokenTtlDays;
    private final long resetTokenTtlMinutes;
    private final String frontendUrl;

    public AuthServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           UserRoleRepository userRoleRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           UserDTOConverter userDTOConverter,
                           @Value("${app.jwt.refresh-token-ttl-days:7}") long refreshTokenTtlDays,
                           @Value("${app.auth.reset-token-ttl-minutes:30}") long resetTokenTtlMinutes,
                           @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userDTOConverter = userDTOConverter;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
        this.resetTokenTtlMinutes = resetTokenTtlMinutes;
        this.frontendUrl = frontendUrl;
    }

    @Override
    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request, String deviceInfo, String ipAddress) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Email đã được sử dụng");
        }
        LocalDateTime now = LocalDateTime.now();

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone() != null && !request.getPhone().isBlank() ? request.getPhone().trim() : null);
        user.setLoyaltyPoints(0);
        user.setIsVerified(false);
        user.setIsActive(true);
        user.setLastLoginAt(now);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user = userRepository.save(user);

        RoleEntity role = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException("Thiếu role " + DEFAULT_ROLE + " trong bảng roles"));
        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setAssignedAt(now);
        userRoleRepository.save(userRole);

        return issueTokens(user, List.of(DEFAULT_ROLE), deviceInfo, ipAddress);
    }

    @Override
    @Transactional
    public AuthResponseDTO login(LoginRequestDTO request, String deviceInfo, String ipAddress) {
        UserEntity user = userRepository.findActiveByEmail(request.getEmail().trim())
                .orElseThrow(() -> ApiException.unauthorized("Email hoặc mật khẩu không đúng"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Email hoặc mật khẩu không đúng");
        }
        ensureActive(user);
        user.setLastLoginAt(LocalDateTime.now());
        return issueTokens(user, userRepository.findRoleNamesByUserId(user.getId()), deviceInfo, ipAddress);
    }

    @Override
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponseDTO refreshToken(String refreshToken, String deviceInfo, String ipAddress) {
        RefreshTokenEntity current = refreshTokenRepository.findByTokenHash(TokenUtil.sha256(refreshToken))
                .orElseThrow(() -> ApiException.unauthorized("Refresh token không hợp lệ"));
        LocalDateTime now = LocalDateTime.now();
        UserEntity user = current.getUser();

        if (current.getRevokedAt() != null) {
            // Token đã bị xoay/thu hồi mà vẫn bị dùng lại -> nghi bị lộ, thu hồi toàn bộ phiên của user.
            refreshTokenRepository.revokeAllByUserId(user.getId(), now);
            throw ApiException.unauthorized("Refresh token đã bị thu hồi, vui lòng đăng nhập lại");
        }
        if (current.getExpiresAt().isBefore(now)) {
            throw ApiException.unauthorized("Refresh token đã hết hạn, vui lòng đăng nhập lại");
        }
        ensureActive(user);

        AuthResponseDTO response = issueTokens(user, userRepository.findRoleNamesByUserId(user.getId()), deviceInfo, ipAddress);
        RefreshTokenEntity replacement = refreshTokenRepository.findByTokenHash(TokenUtil.sha256(response.getRefreshToken()))
                .orElseThrow();
        current.setRevokedAt(now);
        current.setReplacedBy(replacement.getId());
        return response;
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(TokenUtil.sha256(refreshToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(LocalDateTime.now());
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getCurrentUser(UUID userId) {
        UserEntity user = findUser(userId);
        return userDTOConverter.toUserDTO(user, userRepository.findRoleNamesByUserId(userId));
    }

    @Override
    @Transactional
    public void forgotPassword(String email, String ipAddress) {
        // Luôn trả về thành công để không lộ email nào đã đăng ký.
        Optional<UserEntity> userOpt = userRepository.findActiveByEmail(email.trim());
        if (userOpt.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String rawToken = TokenUtil.randomToken();

        PasswordResetTokenEntity token = new PasswordResetTokenEntity();
        token.setUser(userOpt.get());
        token.setTokenHash(TokenUtil.sha256(rawToken));
        token.setExpiresAt(now.plusMinutes(resetTokenTtlMinutes));
        token.setIpAddress(ipAddress);
        token.setCreatedAt(now);
        passwordResetTokenRepository.save(token);

        // TODO: gửi email khi có mail server. Hiện chỉ log link để test.
        log.info("Password reset link for {}: {}/reset-password?token={}", email, frontendUrl, rawToken);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequestDTO request) {
        PasswordResetTokenEntity token = passwordResetTokenRepository.findByTokenHash(TokenUtil.sha256(request.getToken()))
                .orElseThrow(() -> ApiException.badRequest("Token đặt lại mật khẩu không hợp lệ"));
        LocalDateTime now = LocalDateTime.now();
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(now)) {
            throw ApiException.badRequest("Token đặt lại mật khẩu đã hết hạn hoặc đã được sử dụng");
        }
        UserEntity user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(now);
        token.setUsedAt(now);
        refreshTokenRepository.revokeAllByUserId(user.getId(), now);
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequestDTO request) {
        UserEntity user = findUser(userId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("Mật khẩu hiện tại không đúng");
        }
        LocalDateTime now = LocalDateTime.now();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
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

    private void ensureActive(UserEntity user) {
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw ApiException.forbidden("Tài khoản đã bị khoá");
        }
    }

    private AuthResponseDTO issueTokens(UserEntity user, List<String> roles, String deviceInfo, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        String rawRefreshToken = TokenUtil.randomToken();

        RefreshTokenEntity refreshToken = new RefreshTokenEntity();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(TokenUtil.sha256(rawRefreshToken));
        refreshToken.setDeviceInfo(deviceInfo != null && deviceInfo.length() > 255 ? deviceInfo.substring(0, 255) : deviceInfo);
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setExpiresAt(now.plusDays(refreshTokenTtlDays));
        refreshToken.setCreatedAt(now);
        refreshTokenRepository.save(refreshToken);

        AuthResponseDTO response = new AuthResponseDTO();
        response.setAccessToken(jwtService.generateAccessToken(user.getId(), user.getEmail(), roles));
        response.setRefreshToken(rawRefreshToken);
        response.setExpiresIn(jwtService.getAccessTokenTtlSeconds());
        response.setUser(userDTOConverter.toUserDTO(user, roles));
        return response;
    }
}
