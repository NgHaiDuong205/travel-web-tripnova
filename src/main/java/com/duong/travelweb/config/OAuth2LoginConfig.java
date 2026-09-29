package com.duong.travelweb.config;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.security.OAuth2ProviderRegistry;
import com.duong.travelweb.service.OAuthAccountService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Đăng nhập / liên kết Google, Facebook, GitHub. Chain riêng (chỉ /oauth2/** và /login/oauth2/**) được dùng session
 * trong lúc redirect qua lại provider; phần còn lại của API vẫn stateless JWT (SecurityConfig).
 * Kết quả trả về FE qua redirect: {frontend}/oauth2/callback?code=... (đổi lấy JWT ở POST /api/auth/oauth2/exchange/),
 * ?linked={provider} (liên kết xong) hoặc ?error=...
 */
@Configuration
public class OAuth2LoginConfig {
    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginConfig.class);
    private static final String LINK_USER_ATTR = "tripnova.oauth2.linkUserId";

    private final OAuth2ProviderRegistry providerRegistry;
    private final OAuthAccountService oAuthAccountService;
    private final String frontendUrl;

    public OAuth2LoginConfig(OAuth2ProviderRegistry providerRegistry,
                             OAuthAccountService oAuthAccountService,
                             @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl) {
        this.providerRegistry = providerRegistry;
        this.oAuthAccountService = oAuthAccountService;
        this.frontendUrl = frontendUrl;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain oauth2LoginFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/oauth2/**", "/login/oauth2/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        if (providerRegistry.registrations().isEmpty()) {
            // Chưa cấu hình client id/secret nào -> tắt hẳn (FE cũng ẩn nút vì /api/auth/oauth2/providers/ trả rỗng).
            http.authorizeHttpRequests(auth -> auth.anyRequest().denyAll());
            return http.build();
        }
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(new LinkParamFilter(), OAuth2AuthorizationRequestRedirectFilter.class)
                .oauth2Login(oauth -> oauth
                        .clientRegistrationRepository(new InMemoryClientRegistrationRepository(providerRegistry.registrations()))
                        .userInfoEndpoint(u -> u.userService(oauth2UserService()))
                        .successHandler(this::onSuccess)
                        .failureHandler((request, response, ex) -> {
                            log.warn("OAuth2 login failed: {}", ex.getMessage());
                            clearSession(request);
                            redirect(response, "error", "Đăng nhập mạng xã hội bị huỷ hoặc thất bại, vui lòng thử lại");
                        }));
        return http.build();
    }

    private void onSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        HttpSession session = request.getSession(false);
        UUID linkUserId = session == null ? null : (UUID) session.getAttribute(LINK_USER_ATTR);
        clearSession(request);
        try {
            OAuthAccountService.OAuthProfile profile = toProfile((OAuth2AuthenticationToken) authentication);
            String code = oAuthAccountService.completeLogin(profile, linkUserId);
            if (code == null) {
                redirect(response, "linked", profile.provider());
            } else {
                redirect(response, "code", code);
            }
        } catch (ApiException e) {
            redirect(response, "error", e.getMessage());
        }
    }

    /** Chuẩn hoá thuộc tính của từng provider. */
    private OAuthAccountService.OAuthProfile toProfile(OAuth2AuthenticationToken token) {
        String provider = token.getAuthorizedClientRegistrationId();
        Map<String, Object> a = token.getPrincipal().getAttributes();
        return switch (provider) {
            case "google" -> new OAuthAccountService.OAuthProfile(provider, str(a.get("sub")), str(a.get("email")),
                    Boolean.TRUE.equals(a.get("email_verified")), str(a.get("name")), str(a.get("picture")));
            // Facebook chỉ trả email đã xác nhận.
            case "facebook" -> new OAuthAccountService.OAuthProfile(provider, str(a.get("id")), str(a.get("email")),
                    a.get("email") != null, str(a.get("name")), facebookPicture(a.get("picture")));
            case "github" -> new OAuthAccountService.OAuthProfile(provider, str(a.get("id")), str(a.get("email")),
                    Boolean.TRUE.equals(a.get("email_verified")),
                    a.get("name") != null ? str(a.get("name")) : str(a.get("login")), str(a.get("avatar_url")));
            default -> throw ApiException.badRequest("Provider không được hỗ trợ: " + provider);
        };
    }

    /** GitHub: email trên /user có thể ẩn/chưa xác thực -> lấy email primary + verified từ /user/emails. */
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        RestClient github = RestClient.create(providerRegistry.githubApiUrl());
        return request -> {
            OAuth2User user = delegate.loadUser(request);
            if (!"github".equals(request.getClientRegistration().getRegistrationId())) {
                return user;
            }
            Map<String, Object> attributes = new HashMap<>(user.getAttributes());
            attributes.put("email", null);
            attributes.put("email_verified", false);
            try {
                List<Map<String, Object>> emails = github.get().uri("/user/emails")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + request.getAccessToken().getTokenValue())
                        .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {
                        });
                if (emails != null) {
                    // Email primary kèm cờ verified thật của GitHub (chưa xác thực thì không được tự liên kết tài khoản).
                    emails.stream()
                            .filter(e -> Boolean.TRUE.equals(e.get("primary")))
                            .findFirst()
                            .ifPresent(e -> {
                                attributes.put("email", e.get("email"));
                                attributes.put("email_verified", Boolean.TRUE.equals(e.get("verified")));
                            });
                }
            } catch (RuntimeException e) {
                log.warn("Không lấy được email GitHub: {}", e.getMessage());
            }
            return new DefaultOAuth2User(user.getAuthorities(), attributes, "id");
        };
    }

    /**
     * /oauth2/authorization/{provider}?link={token}: lưu userId (đã kiểm tra chữ ký) vào session để onSuccess biết đây là
     * "liên kết thêm" chứ không phải đăng nhập. Không có link -> xoá dấu cũ.
     */
    private class LinkParamFilter extends OncePerRequestFilter {
        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            return !request.getRequestURI().startsWith("/oauth2/authorization/");
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            String link = request.getParameter("link");
            if (link == null || link.isBlank()) {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.removeAttribute(LINK_USER_ATTR);
                }
            } else {
                UUID userId = oAuthAccountService.verifyLinkToken(link);
                if (userId == null) {
                    redirect(response, "error", "Phiên liên kết đã hết hạn, vui lòng thử lại");
                    return;
                }
                request.getSession(true).setAttribute(LINK_USER_ATTR, userId);
            }
            chain.doFilter(request, response);
        }
    }

    private void redirect(HttpServletResponse response, String param, String value) throws IOException {
        response.sendRedirect(UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/callback")
                .queryParam(param, value)
                .encode()
                .build()
                .toUriString());
    }

    private static void clearSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private static String facebookPicture(Object picture) {
        if (picture instanceof Map<?, ?> p && p.get("data") instanceof Map<?, ?> data && data.get("url") != null) {
            return data.get("url").toString();
        }
        return null;
    }

    private static String str(Object value) {
        return value == null ? null : value.toString();
    }
}
