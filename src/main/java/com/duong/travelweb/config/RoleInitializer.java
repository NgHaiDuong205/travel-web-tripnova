package com.duong.travelweb.config;

import com.duong.travelweb.model.entity.RoleEntity;
import com.duong.travelweb.repository.RoleRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/** Đảm bảo các role hệ thống tồn tại trong bảng roles (idempotent, chạy lúc khởi động). */
@Component
public class RoleInitializer implements ApplicationRunner {
    private static final Map<String, String> SYSTEM_ROLES = Map.of(
            "USER", "Khách hàng",
            "ADMIN", "Quản trị viên",
            "HOTEL_MANAGER", "Quản lý khách sạn"
    );

    private final RoleRepository roleRepository;

    public RoleInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
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
    }
}
