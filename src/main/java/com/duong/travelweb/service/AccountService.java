package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.ChangeEmailRequestDTO;
import com.duong.travelweb.model.dto.DeleteAccountRequestDTO;
import com.duong.travelweb.model.dto.UserDTO;
import com.duong.travelweb.model.entity.UserEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/** Xác thực email, đổi email, xoá tài khoản, avatar. */
public interface AccountService {
    /** Gửi (log ra console) link xác thực email. Email đã xác thực -> 400; gửi lại trong vòng 60 giây -> 400. */
    void sendVerification(UUID userId);

    /** Tạo + log link xác thực cho user (dùng khi đăng ký, không kiểm tra thời gian chờ). */
    void issueVerificationLink(UserEntity user);

    /** Token hợp lệ + email trong token trùng email hiện tại -> is_verified = true (gọi lặp lại vẫn OK). */
    UserDTO verifyEmail(String token);

    /** Cần mật khẩu hiện tại; email mới phải chưa ai dùng; sau khi đổi is_verified = false và gửi link xác thực mới. */
    UserDTO changeEmail(UUID userId, ChangeEmailRequestDTO request);

    /** Xoá mềm tài khoản của chính mình (cần mật khẩu); 409 nếu còn booking chưa kết thúc. */
    void deleteAccount(UUID userId, DeleteAccountRequestDTO request);

    UserDTO updateAvatar(UUID userId, MultipartFile file);

    UserDTO removeAvatar(UUID userId);
}
