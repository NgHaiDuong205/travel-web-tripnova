package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.AuthResponseDTO;
import com.duong.travelweb.model.dto.ChangePasswordRequestDTO;
import com.duong.travelweb.model.dto.ForgotPasswordRequestDTO;
import com.duong.travelweb.model.dto.LoginRequestDTO;
import com.duong.travelweb.model.dto.RefreshTokenRequestDTO;
import com.duong.travelweb.model.dto.RegisterRequestDTO;
import com.duong.travelweb.model.dto.ResetPasswordRequestDTO;
import com.duong.travelweb.model.dto.UserDTO;
import com.duong.travelweb.service.AuthService;
import com.duong.travelweb.util.RequestUtil;
import com.duong.travelweb.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthAPI {
    private final AuthService authService;

    public AuthAPI(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/auth/register/")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request,
                                                    HttpServletRequest httpRequest) {
        AuthResponseDTO result = authService.register(request,
                RequestUtil.getUserAgent(httpRequest), RequestUtil.getClientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/api/auth/login/")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request,
                                                 HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request,
                RequestUtil.getUserAgent(httpRequest), RequestUtil.getClientIp(httpRequest)));
    }

    @PostMapping("/api/auth/refresh-token/")
    public ResponseEntity<AuthResponseDTO> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO request,
                                                        HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken(),
                RequestUtil.getUserAgent(httpRequest), RequestUtil.getClientIp(httpRequest)));
    }

    @PostMapping("/api/auth/logout/")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshTokenRequestDTO request) {
        authService.logout(request != null ? request.getRefreshToken() : null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/auth/me/")
    public ResponseEntity<UserDTO> me() {
        return ResponseEntity.ok(authService.getCurrentUser(SecurityUtil.getCurrentUserId()));
    }

    @PostMapping("/api/auth/forgot-password/")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request,
                                               HttpServletRequest httpRequest) {
        authService.forgotPassword(request.getEmail(), RequestUtil.getClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/auth/reset-password/")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/auth/change-password/")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequestDTO request) {
        authService.changePassword(SecurityUtil.getCurrentUserId(), request);
        return ResponseEntity.noContent().build();
    }
}
