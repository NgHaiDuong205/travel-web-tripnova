package com.duong.travelweb.config;

import com.duong.travelweb.model.entity.RoleEntity;
import com.duong.travelweb.model.entity.UserRoleEntity;
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

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final String bootstrapAdminEmail;

    public RoleInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           UserRoleRepository userRoleRepository,
                           @Value("${app.admin.bootstrap-email:}") String bootstrapAdminEmail) {
        this.roleRepository = roleRepository;
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
        grantBootstrapAdmin();
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
