package com.duong.travelweb.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider OAuth2 đang bật. Chỉ bật provider có đủ client id + secret, đọc từ biến môi trường
 * GOOGLE_CLIENT_ID/GOOGLE_CLIENT_SECRET, FACEBOOK_CLIENT_ID/..., GITHUB_CLIENT_ID/... (hoặc app.oauth2.{provider}.client-id/secret).
 * Redirect URI cần khai báo ở console của provider: {BE}/login/oauth2/code/{provider}, VD http://localhost:8080/login/oauth2/code/google.
 */
@Component
public class OAuth2ProviderRegistry {
    private final List<ClientRegistration> registrations = new ArrayList<>();
    private final String githubApiUrl;

    public OAuth2ProviderRegistry(@Value("${app.oauth2.google.client-id:${GOOGLE_CLIENT_ID:}}") String googleId,
                                  @Value("${app.oauth2.google.client-secret:${GOOGLE_CLIENT_SECRET:}}") String googleSecret,
                                  @Value("${app.oauth2.facebook.client-id:${FACEBOOK_CLIENT_ID:}}") String facebookId,
                                  @Value("${app.oauth2.facebook.client-secret:${FACEBOOK_CLIENT_SECRET:}}") String facebookSecret,
                                  @Value("${app.oauth2.github.client-id:${GITHUB_CLIENT_ID:}}") String githubId,
                                  @Value("${app.oauth2.github.client-secret:${GITHUB_CLIENT_SECRET:}}") String githubSecret,
                                  @Value("${app.oauth2.github.web-url:${GITHUB_WEB_URL:}}") String githubWebUrl,
                                  @Value("${app.oauth2.github.api-url:${GITHUB_API_URL:https://api.github.com}}") String githubApiUrl) {
        if (configured(googleId, googleSecret)) {
            registrations.add(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientId(googleId.trim()).clientSecret(googleSecret.trim()).build());
        }
        if (configured(facebookId, facebookSecret)) {
            registrations.add(CommonOAuth2Provider.FACEBOOK.getBuilder("facebook")
                    .clientId(facebookId.trim()).clientSecret(facebookSecret.trim())
                    .userInfoUri("https://graph.facebook.com/me?fields=id,name,email,picture.type(large)")
                    .build());
        }
        this.githubApiUrl = stripSlash(githubApiUrl);
        if (configured(githubId, githubSecret)) {
            // user:email để đọc được email ẩn + cờ verified qua /user/emails
            ClientRegistration.Builder github = CommonOAuth2Provider.GITHUB.getBuilder("github")
                    .clientId(githubId.trim()).clientSecret(githubSecret.trim())
                    .scope("read:user", "user:email");
            if (!githubWebUrl.isBlank()) {
                // GitHub Enterprise Server (hoặc provider giả lập khi test)
                String web = stripSlash(githubWebUrl);
                github.authorizationUri(web + "/login/oauth/authorize")
                        .tokenUri(web + "/login/oauth/access_token")
                        .userInfoUri(this.githubApiUrl + "/user");
            }
            registrations.add(github.build());
        }
    }

    /** API GitHub để đọc /user/emails (mặc định https://api.github.com). */
    public String githubApiUrl() {
        return githubApiUrl;
    }

    private static String stripSlash(String url) {
        String value = url.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public List<ClientRegistration> registrations() {
        return registrations;
    }

    public List<String> enabledProviders() {
        return registrations.stream().map(ClientRegistration::getRegistrationId).toList();
    }

    private static boolean configured(String id, String secret) {
        return id != null && !id.isBlank() && secret != null && !secret.isBlank();
    }
}
