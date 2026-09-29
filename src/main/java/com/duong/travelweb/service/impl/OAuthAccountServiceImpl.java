package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.model.dto.AuthResponseDTO;
import com.duong.travelweb.model.dto.OAuthAccountDTO;
import com.duong.travelweb.model.entity.UserEntity;
import com.duong.travelweb.model.entity.UserOauthAccountEntity;
import com.duong.travelweb.repository.UserOauthAccountRepository;
import com.duong.travelweb.repository.UserRepository;
import com.duong.travelweb.security.SignedTokenService;
import com.duong.travelweb.service.AuthService;
import com.duong.travelweb.service.OAuthAccountService;
import com.duong.travelweb.util.TokenUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OAuthAccountServiceImpl implements OAuthAccountService {
    public static final Set<String> PROVIDERS = Set.of("google", "facebook", "github");
    private static final String LINK_PURPOSE = "oauth-link";
    private static final long LINK_TOKEN_TTL_SECONDS = 300;
    private static final long LOGIN_CODE_TTL_SECONDS = 120;

    private final UserOauthAccountRepository userOauthAccountRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final SignedTokenService signedTokenService;
    /**
     * Mã đăng nhập dùng 1 lần (sha256(code) -> user + hạn). Giữ trong bộ nhớ: chỉ sống 2 phút, 1 instance BE;
     * không đưa JWT lên URL redirect để tránh lộ qua lịch sử trình duyệt / Referer.
     */
    private final Map<String, PendingLogin> loginCodes = new ConcurrentHashMap<>();

    private record PendingLogin(UUID userId, Instant expiresAt) {
    }

    public OAuthAccountServiceImpl(UserOauthAccountRepository userOauthAccountRepository,
                                   UserRepository userRepository,
                                   AuthService authService,
                                   SignedTokenService signedTokenService) {
        this.userOauthAccountRepository = userOauthAccountRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.signedTokenService = signedTokenService;
    }

    @Override
    @Transactional
    public String completeLogin(OAuthProfile profile, UUID linkUserId) {
        String providerName = label(profile.provider());
        UserOauthAccountEntity existing = userOauthAccountRepository
                .findByProviderAccount(profile.provider(), profile.providerUserId())
                .orElse(null);

        if (linkUserId != null) {
            UserEntity user = findActiveUser(linkUserId);
            if (existing != null && !existing.getUserId().equals(user.getId())) {
                throw ApiException.conflict("Tài khoản " + providerName + " này đã được liên kết với một tài khoản TripNova khác");
            }
            UserOauthAccountEntity sameProvider = userOauthAccountRepository
                    .findByUserIdAndProvider(user.getId(), profile.provider())
                    .orElse(null);
            if (existing == null && sameProvider != null) {
                throw ApiException.conflict("Bạn đã liên kết một tài khoản " + providerName + " khác, hãy gỡ liên kết trước");
            }
            saveLink(existing, user.getId(), profile);
            return null;
        }

        UUID userId;
        if (existing != null) {
            userId = findActiveUser(existing.getUserId()).getId();
            saveLink(existing, userId, profile);
        } else {
            if (profile.email() == null || profile.email().isBlank()) {
                throw ApiException.badRequest(providerName + " không chia sẻ email. Hãy đăng ký bằng email rồi liên kết "
                        + providerName + " trong phần cài đặt tài khoản");
            }
            UserEntity byEmail = userRepository.findActiveByEmail(profile.email().trim()).orElse(null);
            if (byEmail != null) {
                // Chỉ tự liên kết khi provider xác nhận email — nếu không, ai tạo tài khoản provider với email của người
                // khác sẽ chiếm được tài khoản TripNova của họ.
                if (!profile.emailVerified()) {
                    throw ApiException.conflict("Email " + profile.email() + " đã có tài khoản. Hãy đăng nhập bằng mật khẩu rồi liên kết "
                            + providerName + " trong phần cài đặt tài khoản");
                }
                userId = requireActive(byEmail).getId();
            } else if (userRepository.existsByEmailIgnoreCase(profile.email().trim())) {
                throw ApiException.conflict("Email " + profile.email() + " thuộc một tài khoản đã bị xoá");
            } else {
                String name = profile.displayName() != null && !profile.displayName().isBlank()
                        ? profile.displayName().trim() : profile.email().trim().split("@")[0];
                userId = authService.registerOAuthUser(profile.email(), name.length() > 150 ? name.substring(0, 150) : name,
                        profile.avatarUrl(), profile.emailVerified());
            }
            saveLink(null, userId, profile);
        }

        purgeExpiredCodes();
        String code = TokenUtil.randomToken();
        loginCodes.put(TokenUtil.sha256(code), new PendingLogin(userId, Instant.now().plusSeconds(LOGIN_CODE_TTL_SECONDS)));
        return code;
    }

    @Override
    public AuthResponseDTO exchangeLoginCode(String code, String deviceInfo, String ipAddress) {
        PendingLogin pending = code == null ? null : loginCodes.remove(TokenUtil.sha256(code));
        if (pending == null || pending.expiresAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("Mã đăng nhập không hợp lệ hoặc đã hết hạn, vui lòng thử lại");
        }
        return authService.issueTokensForUser(pending.userId(), deviceInfo, ipAddress);
    }

    @Override
    public String createLinkToken(UUID userId) {
        UserEntity user = findActiveUser(userId);
        return signedTokenService.sign(LINK_PURPOSE, user.getId(), user.getEmail(), LINK_TOKEN_TTL_SECONDS);
    }

    @Override
    public UUID verifyLinkToken(String token) {
        return signedTokenService.verify(LINK_PURPOSE, token).map(SignedTokenService.Claims::userId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OAuthAccountDTO> findMine(UUID userId) {
        return userOauthAccountRepository.findByUserId(userId).stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public void unlink(UUID userId, String provider) {
        String name = provider == null ? "" : provider.trim().toLowerCase();
        UserOauthAccountEntity account = userOauthAccountRepository.findByUserIdAndProvider(userId, name)
                .orElseThrow(() -> ApiException.notFound("Chưa liên kết tài khoản " + label(name)));
        userOauthAccountRepository.delete(account);
    }

    private void saveLink(UserOauthAccountEntity existing, UUID userId, OAuthProfile profile) {
        UserOauthAccountEntity account = existing != null ? existing : new UserOauthAccountEntity();
        account.setUserId(userId);
        account.setProvider(profile.provider());
        account.setProviderUserId(profile.providerUserId());
        account.setEmail(profile.email());
        account.setDisplayName(profile.displayName());
        account.setAvatarUrl(profile.avatarUrl());
        userOauthAccountRepository.save(account);
    }

    private UserEntity findActiveUser(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> ApiException.forbidden("Tài khoản không còn tồn tại"));
        return requireActive(user);
    }

    private UserEntity requireActive(UserEntity user) {
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw ApiException.forbidden("Tài khoản đã bị khoá");
        }
        return user;
    }

    private void purgeExpiredCodes() {
        Instant now = Instant.now();
        loginCodes.values().removeIf(p -> p.expiresAt().isBefore(now));
    }

    private static String label(String provider) {
        return switch (provider) {
            case "google" -> "Google";
            case "facebook" -> "Facebook";
            case "github" -> "GitHub";
            default -> provider;
        };
    }

    private OAuthAccountDTO toDTO(UserOauthAccountEntity entity) {
        OAuthAccountDTO dto = new OAuthAccountDTO();
        dto.setProvider(entity.getProvider());
        dto.setEmail(entity.getEmail());
        dto.setDisplayName(entity.getDisplayName());
        dto.setAvatarUrl(entity.getAvatarUrl());
        dto.setLinkedAt(entity.getLinkedAt());
        return dto;
    }
}
