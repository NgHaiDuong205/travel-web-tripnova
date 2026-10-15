package com.duong.travelweb.security;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.entity.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * Tài khoản khách phải xác thực email mới được đăng nhập.
 * Chỉ áp dụng cho tài khoản tạo từ `app.auth.email-verification-required-since` trở đi (tài khoản cũ giữ nguyên,
 * không sửa dữ liệu của họ) và chỉ có role USER — admin / tài khoản khách sạn do admin tạo được miễn.
 */
@Component
public class EmailVerificationPolicy {
    public static final String ERROR_CODE = "email_not_verified";

    private final boolean enabled;
    private final LocalDateTime requiredSince;

    public EmailVerificationPolicy(@Value("${app.auth.require-email-verification:true}") boolean enabled,
                                   @Value("${app.auth.email-verification-required-since:2026-10-15T00:00:00}") LocalDateTime requiredSince) {
        this.enabled = enabled;
        this.requiredSince = requiredSince;
    }

    public boolean isRequired(UserEntity user, Collection<String> roles) {
        if (!enabled || Boolean.TRUE.equals(user.getIsVerified())) {
            return false;
        }
        if (user.getCreatedAt() == null || user.getCreatedAt().isBefore(requiredSince)) {
            return false;
        }
        return roles.stream().allMatch("USER"::equals);
    }

    /** 403 + code email_not_verified (FE hiện nút gửi lại link). */
    public void check(UserEntity user, Collection<String> roles) {
        if (isRequired(user, roles)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Vui lòng xác thực email trước khi đăng nhập. Hãy mở link chúng tôi đã gửi tới " + user.getEmail() + ".",
                    ERROR_CODE);
        }
    }
}
