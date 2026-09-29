package com.duong.travelweb.service.impl;

import com.duong.travelweb.exception.ApiException;
import com.duong.travelweb.converter.UserDTOConverter;
import com.duong.travelweb.model.dto.ChangeEmailRequestDTO;
import com.duong.travelweb.model.dto.DeleteAccountRequestDTO;
import com.duong.travelweb.model.dto.UserDTO;
import com.duong.travelweb.model.entity.UserEntity;
import com.duong.travelweb.repository.RefreshTokenRepository;
import com.duong.travelweb.repository.UserOauthAccountRepository;
import com.duong.travelweb.repository.UserRepository;
import com.duong.travelweb.security.SignedTokenService;
import com.duong.travelweb.service.AccountService;
import com.duong.travelweb.service.UploadService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AccountServiceImpl implements AccountService {
    public static final String VERIFY_EMAIL_PURPOSE = "verify-email";
    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);
    private static final long VERIFY_TOKEN_TTL_SECONDS = 24 * 3600;
    private static final long RESEND_COOLDOWN_SECONDS = 60;
    /** Booking còn mở ở bất kỳ loại nào -> chưa cho xoá tài khoản. */
    private static final String OPEN_BOOKING_SQL = """
            SELECT EXISTS (SELECT 1 FROM hotel_bookings WHERE user_id = :u AND status IN ('pending', 'confirmed', 'checked_in'))
                OR EXISTS (SELECT 1 FROM car_bookings WHERE user_id = :u AND status IN ('pending', 'confirmed', 'checked_in'))
                OR EXISTS (SELECT 1 FROM flight_bookings WHERE user_id = :u AND status IN ('pending', 'confirmed', 'checked_in'))
                OR EXISTS (SELECT 1 FROM tour_bookings WHERE user_id = :u AND status IN ('pending', 'confirmed', 'checked_in'))
            """;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserOauthAccountRepository userOauthAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final SignedTokenService signedTokenService;
    private final UploadService uploadService;
    private final UserDTOConverter userDTOConverter;
    private final String frontendUrl;
    /** userId -> thời điểm gửi link gần nhất (chỉ chống bấm liên tục, mất khi khởi động lại là chấp nhận được). */
    private final Map<UUID, Instant> lastVerificationSent = new ConcurrentHashMap<>();

    @PersistenceContext
    private EntityManager entityManager;

    public AccountServiceImpl(UserRepository userRepository,
                              RefreshTokenRepository refreshTokenRepository,
                              UserOauthAccountRepository userOauthAccountRepository,
                              PasswordEncoder passwordEncoder,
                              SignedTokenService signedTokenService,
                              UploadService uploadService,
                              UserDTOConverter userDTOConverter,
                              @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userOauthAccountRepository = userOauthAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.signedTokenService = signedTokenService;
        this.uploadService = uploadService;
        this.userDTOConverter = userDTOConverter;
        this.frontendUrl = frontendUrl;
    }

    @Override
    @Transactional(readOnly = true)
    public void sendVerification(UUID userId) {
        UserEntity user = findUser(userId);
        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw ApiException.badRequest("Email đã được xác thực");
        }
        Instant now = Instant.now();
        Instant last = lastVerificationSent.get(userId);
        if (last != null && last.plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(now)) {
            throw ApiException.badRequest("Vui lòng đợi " + RESEND_COOLDOWN_SECONDS + " giây trước khi gửi lại");
        }
        lastVerificationSent.put(userId, now);
        issueVerificationLink(user);
    }

    @Override
    public void issueVerificationLink(UserEntity user) {
        String token = signedTokenService.sign(VERIFY_EMAIL_PURPOSE, user.getId(), user.getEmail(), VERIFY_TOKEN_TTL_SECONDS);
        // TODO: gửi email khi có mail server. Hiện chỉ log link để test (giống forgot-password).
        log.info("Email verification link for {}: {}/verify-email?token={}", user.getEmail(), frontendUrl, token);
    }

    @Override
    @Transactional
    public UserDTO verifyEmail(String token) {
        SignedTokenService.Claims claims = signedTokenService.verify(VERIFY_EMAIL_PURPOSE, token)
                .orElseThrow(() -> ApiException.badRequest("Link xác thực không hợp lệ hoặc đã hết hạn"));
        UserEntity user = userRepository.findById(claims.userId())
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> ApiException.badRequest("Link xác thực không hợp lệ hoặc đã hết hạn"));
        // Đã đổi email sau khi gửi link -> link cũ vô hiệu.
        if (!user.getEmail().equalsIgnoreCase(claims.email())) {
            throw ApiException.badRequest("Link xác thực không còn hiệu lực vì email đã thay đổi");
        }
        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            user.setIsVerified(true);
            user.setUpdatedAt(LocalDateTime.now());
        }
        return toDTO(user);
    }

    @Override
    @Transactional
    public UserDTO changeEmail(UUID userId, ChangeEmailRequestDTO request) {
        UserEntity user = findUser(userId);
        requirePassword(user, request.getCurrentPassword(), "Mật khẩu hiện tại không đúng");
        String email = request.getNewEmail().trim().toLowerCase();
        if (email.equalsIgnoreCase(user.getEmail())) {
            throw ApiException.badRequest("Email mới trùng email hiện tại");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Email đã được sử dụng");
        }
        log.info("[MAIL] To: {} | Email của tài khoản TripNova đã được đổi sang {}", user.getEmail(), email);
        user.setEmail(email);
        user.setIsVerified(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.flush(); // lỗi unique (2 request cùng lúc) nổ ở đây -> 409 qua ControllerAdvisor
        lastVerificationSent.put(userId, Instant.now());
        issueVerificationLink(user);
        return toDTO(user);
    }

    @Override
    @Transactional
    public void deleteAccount(UUID userId, DeleteAccountRequestDTO request) {
        UserEntity user = findUser(userId);
        requirePassword(user, request.getPassword(), "Mật khẩu không đúng");
        boolean hasOpenBooking = (Boolean) entityManager.createNativeQuery(OPEN_BOOKING_SQL)
                .setParameter("u", userId)
                .getSingleResult();
        if (hasOpenBooking) {
            throw ApiException.conflict("Bạn còn đơn đặt chưa kết thúc (chờ thanh toán / đã xác nhận). Hãy huỷ hoặc đợi hoàn tất trước khi xoá tài khoản");
        }
        LocalDateTime now = LocalDateTime.now();
        user.setDeletedAt(now);
        user.setIsActive(false);
        user.setUpdatedAt(now);
        refreshTokenRepository.revokeAllByUserId(userId, now);
        // Bỏ liên kết mạng xã hội để đăng nhập OAuth không mở lại tài khoản đã xoá.
        userOauthAccountRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional
    public UserDTO updateAvatar(UUID userId, MultipartFile file) {
        UserEntity user = findUser(userId);
        String url = uploadService.storeImage(userId, file);
        String old = user.getAvatarUrl();
        user.setAvatarUrl(url);
        user.setUpdatedAt(LocalDateTime.now());
        afterCommit(() -> uploadService.deleteQuietly(old), () -> uploadService.deleteQuietly(url));
        return toDTO(user);
    }

    @Override
    @Transactional
    public UserDTO removeAvatar(UUID userId) {
        UserEntity user = findUser(userId);
        String old = user.getAvatarUrl();
        user.setAvatarUrl(null);
        user.setUpdatedAt(LocalDateTime.now());
        afterCommit(() -> uploadService.deleteQuietly(old), () -> { });
        return toDTO(user);
    }

    /** Xoá file cũ chỉ khi DB đã commit; rollback thì dọn file vừa lưu. */
    private void afterCommit(Runnable onCommit, Runnable onRollback) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    onCommit.run();
                } else {
                    onRollback.run();
                }
            }
        });
    }

    private void requirePassword(UserEntity user, String password, String message) {
        if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw ApiException.badRequest(message);
        }
    }

    private UserEntity findUser(UUID userId) {
        return userRepository.findById(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy người dùng"));
    }

    private UserDTO toDTO(UserEntity user) {
        return userDTOConverter.toUserDTO(user, userRepository.findRoleNamesByUserId(user.getId()));
    }
}
