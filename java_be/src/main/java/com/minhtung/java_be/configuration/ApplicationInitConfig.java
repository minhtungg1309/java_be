package com.minhtung.java_be.configuration;

import com.minhtung.java_be.constant.PredefinedRole;
import com.minhtung.java_be.entity.Role;
import com.minhtung.java_be.entity.User;
import com.minhtung.java_be.repository.RoleRepository;
import com.minhtung.java_be.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    @NonFinal
    static final String ADMIN_USER_NAME = "admin";

    UserRepository userRepository;
    RoleRepository roleRepository;
    PasswordEncoder passwordEncoder;

    /**
     * ✅ Legacy Seeder - Chỉ chạy khi DataSeeder chưa hoạt động
     * Đây là backup seeder cho trường hợp DataSeeder fail
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "spring",
            value = "datasource.driverClassName",
            havingValue = "com.mysql.cj.jdbc.Driver")
    ApplicationRunner applicationRunner() {
        log.info("🔄 Initializing ApplicationInitConfig (Legacy Seeder)...");
        return args -> {
            // ✅ Chỉ tạo admin user nếu chưa có
            if (userRepository.findByUsername(ADMIN_USER_NAME).isEmpty()) {
                log.info("👤 Creating admin user via ApplicationInitConfig...");
                
                // Tìm admin role
                Role adminRole = roleRepository.findById(PredefinedRole.ADMIN_ROLE)
                    .orElseGet(() -> {
                        log.warn("⚠️ Admin role not found, creating basic admin role...");
                        Role role = new Role();
                        role.setName(PredefinedRole.ADMIN_ROLE);
                        role.setDescription("Administrator role");
                        return roleRepository.save(role);
                    });

                // Tạo admin user
                User adminUser = new User();
                adminUser.setUsername(ADMIN_USER_NAME);
                adminUser.setPassword(passwordEncoder.encode(ADMIN_USER_NAME));
                adminUser.setFirstName("Admin");
                adminUser.setLastName("User");
                adminUser.setRoles(new HashSet<>());
                adminUser.getRoles().add(adminRole);

                userRepository.save(adminUser);
                log.warn("⚠️ Admin user created with username: admin, password: admin - PLEASE CHANGE IN PRODUCTION!");
            } else {
                log.info("✅ Admin user already exists, ApplicationInitConfig skipping...");
            }
        };
    }
}
