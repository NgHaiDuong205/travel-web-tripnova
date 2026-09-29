package com.duong.travelweb.config;

import com.duong.travelweb.model.entity.PermissionEntity;
import com.duong.travelweb.model.entity.RoleEntity;
import com.duong.travelweb.model.entity.UserRoleEntity;
import com.duong.travelweb.repository.PermissionRepository;
import com.duong.travelweb.repository.RoleRepository;
import com.duong.travelweb.repository.UserRepository;
import com.duong.travelweb.repository.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Chạy lúc khởi động (idempotent):
 * - đảm bảo các role hệ thống tồn tại trong bảng roles;
 * - cấp ADMIN cho user có email = app.admin.bootstrap-email (nếu user đã đăng ký) để có admin đầu tiên.
 */
@Component
public class RoleInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(RoleInitializer.class);
    private static final Map<String, String> SYSTEM_ROLES = Map.of(
            "USER", "Khách hàng",
            "ADMIN", "Quản trị viên",
            "HOTEL_MANAGER", "Quản lý khách sạn"
    );
    private static final Map<String, String> PERMISSIONS = Map.of(
            "dashboard.view", "Xem thống kê tổng quan",
            "hotel.manage", "Quản lý khách sạn, loại phòng, phòng",
            "booking.manage", "Quản lý đặt phòng",
            "payment.manage", "Quản lý giao dịch và hoàn tiền",
            "user.manage", "Quản lý người dùng và phân quyền",
            "geography.manage", "Quản lý châu lục, quốc gia, điểm đến, địa danh",
            "amenity.manage", "Quản lý tiện nghi",
            "contact.manage", "Xử lý tin nhắn liên hệ",
            "review.manage", "Kiểm duyệt đánh giá và bình luận",
            "invoice.manage", "Xem và gửi lại hoá đơn"
    );

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final String bootstrapAdminEmail;

    public RoleInitializer(RoleRepository roleRepository,
                           PermissionRepository permissionRepository,
                           UserRepository userRepository,
                           UserRoleRepository userRoleRepository,
                           @Value("${app.admin.bootstrap-email:}") String bootstrapAdminEmail) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.bootstrapAdminEmail = bootstrapAdminEmail;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        SYSTEM_ROLES.forEach((name, description) -> {
            if (roleRepository.findByName(name).isEmpty()) {
                RoleEntity role = new RoleEntity();
                role.setName(name);
                role.setDescription(description);
                role.setCreatedAt(LocalDateTime.now());
                roleRepository.save(role);
            }
        });
        seedPermissions();
        grantBootstrapAdmin();
    }

    /** Thêm các quyền còn thiếu vào danh mục; quyền mới được gán luôn cho ADMIN. */
    private void seedPermissions() {
        RoleEntity admin = roleRepository.findByName("ADMIN").orElseThrow();
        PERMISSIONS.forEach((code, name) -> {
            if (permissionRepository.existsByCode(code)) {
                return;
            }
            PermissionEntity permission = new PermissionEntity();
            permission.setCode(code);
            permission.setName(name);
            permission.setCreatedAt(LocalDateTime.now());
            permissionRepository.saveAndFlush(permission);
            permissionRepository.insertLink(admin.getId(), permission.getId());
            log.info("Seeded permission {}", code);
        });
    }

    private void grantBootstrapAdmin() {
        if (bootstrapAdminEmail == null || bootstrapAdminEmail.isBlank()) {
            return;
        }
        userRepository.findActiveByEmail(bootstrapAdminEmail.trim()).ifPresentOrElse(user -> {
            if (userRepository.findRoleNamesByUserId(user.getId()).contains("ADMIN")) {
                return;
            }
            UserRoleEntity userRole = new UserRoleEntity();
            userRole.setUser(user);
            userRole.setRole(roleRepository.findByName("ADMIN").orElseThrow());
            userRole.setAssignedAt(LocalDateTime.now());
            userRoleRepository.save(userRole);
            log.info("Granted ADMIN role to bootstrap user {}", bootstrapAdminEmail);
        }, () -> log.warn("Bootstrap admin {} chưa đăng ký tài khoản — đăng ký rồi khởi động lại app", bootstrapAdminEmail));
    }
}
