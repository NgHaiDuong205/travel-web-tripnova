package com.duong.travelweb.api;

import com.duong.travelweb.model.dto.AuthResponseDTO;
import com.duong.travelweb.model.dto.OAuthAccountDTO;
import com.duong.travelweb.security.OAuth2ProviderRegistry;
import com.duong.travelweb.service.OAuthAccountService;
import com.duong.travelweb.util.RequestUtil;
import com.duong.travelweb.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Đăng nhập mạng xã hội: FE mở {BE}/oauth2/authorization/{provider} (xem OAuth2LoginConfig), BE redirect về
 * {FE}/oauth2/callback?code=... rồi FE gọi /api/auth/oauth2/exchange/ để lấy JWT.
 */
@RestController
public class OAuthAPI {
    private final OAuthAccountService oAuthAccountService;
    private final OAuth2ProviderRegistry providerRegistry;

    public OAuthAPI(OAuthAccountService oAuthAccountService, OAuth2ProviderRegistry providerRegistry) {
        this.oAuthAccountService = oAuthAccountService;
        this.providerRegistry = providerRegistry;
    }

    /** Public: provider đã cấu hình client id/secret (rỗng -> FE ẩn nút). */
    @GetMapping("/api/auth/oauth2/providers/")
    public ResponseEntity<List<String>> getProviders() {
        return ResponseEntity.ok(providerRegistry.enabledProviders());
    }

    /** Public: đổi mã dùng 1 lần (2 phút) lấy access + refresh token. */
    @PostMapping("/api/auth/oauth2/exchange/")
    public ResponseEntity<AuthResponseDTO> exchange(@RequestBody Map<String, String> body, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(oAuthAccountService.exchangeLoginCode(body.get("code"),
                RequestUtil.getUserAgent(httpRequest), RequestUtil.getClientIp(httpRequest)));
    }

    @GetMapping("/api/me/oauth-accounts/")
    public ResponseEntity<List<OAuthAccountDTO>> getMyAccounts() {
        return ResponseEntity.ok(oAuthAccountService.findMine(SecurityUtil.getCurrentUserId()));
    }

    /** Token 5 phút để mở /oauth2/authorization/{provider}?link={token} (liên kết thêm provider cho user đang đăng nhập). */
    @PostMapping("/api/me/oauth-accounts/link-token/")
    public ResponseEntity<Map<String, String>> createLinkToken() {
        return ResponseEntity.ok(Map.of("token", oAuthAccountService.createLinkToken(SecurityUtil.getCurrentUserId())));
    }

    @DeleteMapping("/api/me/oauth-accounts/{provider}/")
    public ResponseEntity<Void> unlink(@PathVariable("provider") String provider) {
        oAuthAccountService.unlink(SecurityUtil.getCurrentUserId(), provider);
        return ResponseEntity.noContent().build();
    }
}
