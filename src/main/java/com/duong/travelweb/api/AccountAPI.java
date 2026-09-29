package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.ChangeEmailRequestDTO;
import com.duong.travelweb.model.dto.DeleteAccountRequestDTO;
import com.duong.travelweb.model.dto.UserDTO;
import com.duong.travelweb.model.dto.VerifyEmailRequestDTO;
import com.duong.travelweb.service.AccountService;
import com.duong.travelweb.service.UploadService;
import com.duong.travelweb.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/** Xác thực email, đổi email, xoá tài khoản, avatar và upload ảnh. */
@RestController
public class AccountAPI {
    private final AccountService accountService;
    private final UploadService uploadService;

    public AccountAPI(AccountService accountService, UploadService uploadService) {
        this.accountService = accountService;
        this.uploadService = uploadService;
    }

    /** Public: token lấy từ link xác thực. */
    @PostMapping("/api/auth/verify-email/")
    public ResponseEntity<UserDTO> verifyEmail(@Valid @RequestBody VerifyEmailRequestDTO request) {
        return ResponseEntity.ok(accountService.verifyEmail(request.getToken().trim()));
    }

    @PostMapping("/api/auth/resend-verification/")
    public ResponseEntity<Void> resendVerification() {
        accountService.sendVerification(SecurityUtil.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/account/email/")
    public ResponseEntity<UserDTO> changeEmail(@Valid @RequestBody ChangeEmailRequestDTO request) {
        return ResponseEntity.ok(accountService.changeEmail(SecurityUtil.getCurrentUserId(), request));
    }

    @DeleteMapping("/api/account/")
    public ResponseEntity<Void> deleteAccount(@Valid @RequestBody DeleteAccountRequestDTO request) {
        accountService.deleteAccount(SecurityUtil.getCurrentUserId(), request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/api/me/avatar/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDTO> updateAvatar(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(accountService.updateAvatar(SecurityUtil.getCurrentUserId(), file));
    }

    @DeleteMapping("/api/me/avatar/")
    public ResponseEntity<UserDTO> removeAvatar() {
        return ResponseEntity.ok(accountService.removeAvatar(SecurityUtil.getCurrentUserId()));
    }

    /** Trả {url} tuyệt đối để dùng cho ảnh review, ảnh khách sạn... */
    @PostMapping(value = "/api/uploads/images/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = uploadService.storeImage(SecurityUtil.getCurrentUserId(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("url", url));
    }

    @DeleteMapping("/api/uploads/images/")
    public ResponseEntity<Void> deleteImage(@RequestBody Map<String, String> body) {
        uploadService.deleteImage(SecurityUtil.getCurrentUserId(), SecurityUtil.hasRole("ADMIN"), body.get("url"));
        return ResponseEntity.noContent().build();
    }
}
