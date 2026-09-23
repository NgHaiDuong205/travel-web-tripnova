package com.duong.travelweb.util;

import com.duong.travelweb.exception.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public class SecurityUtil {

    /** userId (claim "sub") của user đang đăng nhập, ném 401 nếu chưa đăng nhập. */
    public static UUID getCurrentUserId() {
        UUID userId = findCurrentUserId();
        if (userId == null) {
            throw ApiException.unauthorized("Bạn chưa đăng nhập");
        }
        return userId;
    }

    public static UUID findCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return UUID.fromString(jwt.getSubject());
        }
        return null;
    }

    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        String authority = "ROLE_" + role;
        return authentication.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
