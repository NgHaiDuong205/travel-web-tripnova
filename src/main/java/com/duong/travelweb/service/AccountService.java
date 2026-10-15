package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.ChangeEmailRequestDTO;
import com.duong.travelweb.model.dto.DeleteAccountRequestDTO;
import com.duong.travelweb.model.dto.UserDTO;
import com.duong.travelweb.model.entity.UserEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/** Xác thực email, đổi email, xoá tài khoản, avatar. */
public interface AccountService {
    /** Gửi lại email xác thực cho user đang đăng nhập. Email đã xác thực -> 400; gửi lại trong vòng 60 giây -> 400. */
    void sendVerification(UUID userId);

    /**
     * Gửi lại email xác thực theo địa chỉ email (chưa đăng nhập được vì chưa xác thực). Không báo lỗi gì
     * (email không tồn tại / đã xác thực / đang chờ 60 giây) để không lộ email nào đã đăng ký.
     */
    void resendVerificationByEmail(String email);

    /** Gửi link xác thực (đổi email; không kiểm tra thời gian chờ). */
    void issueVerificationLink(UserEntity user);

    /** Email chào mừng kèm link xác thực (khi đăng ký). */
    void issueWelcomeVerification(UserEntity user);

    /** Token hợp lệ + email trong token trùng email hiện tại -> is_verified = true (gọi lặp lại vẫn OK). */
    UserDTO verifyEmail(String token);

    /** Cần mật khẩu hiện tại; email mới phải chưa ai dùng; sau khi đổi is_verified = false và gửi link xác thực mới. */
    UserDTO changeEmail(UUID userId, ChangeEmailRequestDTO request);

    /** Xoá mềm tài khoản của chính mình (cần mật khẩu); 409 nếu còn booking chưa kết thúc. */
    void deleteAccount(UUID userId, DeleteAccountRequestDTO request);

    UserDTO updateAvatar(UUID userId, MultipartFile file);

    UserDTO removeAvatar(UUID userId);
}
