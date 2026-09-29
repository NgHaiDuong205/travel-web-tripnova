package com.duong.travelweb.service;

import com.duong.travelweb.model.dto.AuthResponseDTO;
import com.duong.travelweb.model.dto.OAuthAccountDTO;

import java.util.List;
import java.util.UUID;

/** Liên kết tài khoản mạng xã hội (bảng user_oauth_accounts) và đăng nhập qua OAuth2. */
public interface OAuthAccountService {
    /** Thông tin lấy từ provider sau khi user đồng ý. email có thể null (provider không chia sẻ). */
    record OAuthProfile(String provider, String providerUserId, String email, boolean emailVerified, String displayName,
                        String avatarUrl) {
    }

    /**
     * linkUserId != null: gắn tài khoản provider vào user đang đăng nhập, trả về null.
     * linkUserId == null: đăng nhập — tìm theo liên kết, rồi theo email đã xác thực (tự liên kết), không có thì tạo user mới;
     * trả về mã dùng 1 lần (hết hạn sau 2 phút) để FE đổi lấy JWT qua {@link #exchangeLoginCode}.
     * Lỗi nghiệp vụ -> ApiException (thông điệp hiển thị cho user).
     */
    String completeLogin(OAuthProfile profile, UUID linkUserId);

    AuthResponseDTO exchangeLoginCode(String code, String deviceInfo, String ipAddress);

    /** Token ngắn hạn cho luồng "liên kết": FE gắn vào /oauth2/authorization/{provider}?link=... */
    String createLinkToken(UUID userId);

    /** userId trong link token, null nếu không hợp lệ / hết hạn. */
    UUID verifyLinkToken(String token);

    List<OAuthAccountDTO> findMine(UUID userId);

    void unlink(UUID userId, String provider);
}
