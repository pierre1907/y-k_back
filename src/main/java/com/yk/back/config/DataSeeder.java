package com.yk.back.config;

import com.yk.back.entity.PlatformAdmin;
import com.yk.back.repository.PlatformAdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final PlatformAdminRepository platformAdminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-email:admin@yk-platform.com}")
    private String adminEmail;

    @Value("${app.seed.admin-password:Admin@YK2024!}")
    private String adminPassword;

    @Value("${app.seed.admin-name:Super Admin}")
    private String adminName;

    @Override
    public void run(ApplicationArguments args) {
        if (!platformAdminRepository.existsByEmail(adminEmail)) {
            PlatformAdmin admin = PlatformAdmin.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .fullName(adminName)
                    .build();
            platformAdminRepository.save(admin);
            log.info("Super admin créé : {}", adminEmail);
        }
    }
}
