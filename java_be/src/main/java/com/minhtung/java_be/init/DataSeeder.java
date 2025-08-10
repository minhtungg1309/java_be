package com.minhtung.java_be.init;

import com.minhtung.java_be.constant.PredefinedRole;
import com.minhtung.java_be.entity.Permission;
import com.minhtung.java_be.entity.Role;
import com.minhtung.java_be.entity.User;
import com.minhtung.java_be.repository.PermissionRepository;
import com.minhtung.java_be.repository.RoleRepository;
import com.minhtung.java_be.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DataSeeder {

    UserRepository userRepository;
    RoleRepository roleRepository;
    PermissionRepository permissionRepository;
    PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    public void seedData() {
        log.info("🔄 Starting DataSeeder (Main Seeder)...");
        
        // ✅ Kiểm tra xem đã có data chưa
        if (isDataExists()) {
            log.info("✅ Data already exists, DataSeeder skipping...");
            return;
        }

        log.info("🌱 No data found, starting comprehensive seeding...");
        try {
            seedPermissions();
            seedRoles();
            seedUsers();
            log.info("✅ DataSeeder completed successfully!");
        } catch (Exception e) {
            log.error("❌ DataSeeder failed: {}", e.getMessage(), e);
        }
    }

    private boolean isDataExists() {
        long userCount = userRepository.count();
        long roleCount = roleRepository.count();
        long permissionCount = permissionRepository.count();
        
        log.info("📊 Current data count - Users: {}, Roles: {}, Permissions: {}", 
                userCount, roleCount, permissionCount);
        
        // ✅ Chỉ skip nếu có đầy đủ data (users, roles, permissions)
        boolean hasData = userCount > 0 && roleCount > 0 && permissionCount > 0;
        if (hasData) {
            log.info("✅ Sufficient data exists, skipping seeding");
        } else {
            log.info("⚠️ Insufficient data, proceeding with seeding");
        }
        return hasData;
    }

    private void seedPermissions() {
        log.info("🔐 Seeding permissions...");
        List<Permission> permissions = Arrays.asList(
            createPermission("USER_READ", "Read user information"),
            createPermission("USER_WRITE", "Create/Update user"),
            createPermission("USER_DELETE", "Delete user"),
            createPermission("ROLE_READ", "Read roles"),
            createPermission("ROLE_WRITE", "Create/Update roles"),
            createPermission("ROLE_DELETE", "Delete roles"),
            createPermission("PERMISSION_READ", "Read permissions"),
            createPermission("PERMISSION_WRITE", "Create/Update permissions"),
            createPermission("PERMISSION_DELETE", "Delete permissions")
        );
        permissionRepository.saveAll(permissions);
        log.info("✅ Permissions seeded successfully");
    }

    private void seedRoles() {
        log.info("👥 Seeding roles...");
        
        // Lấy permissions
        Set<Permission> userPermissions = getPermissionsByName(Arrays.asList("USER_READ"));
        Set<Permission> adminPermissions = getPermissionsByName(Arrays.asList(
            "USER_READ", "USER_WRITE", "USER_DELETE",
            "ROLE_READ", "ROLE_WRITE", "ROLE_DELETE",
            "PERMISSION_READ", "PERMISSION_WRITE", "PERMISSION_DELETE"
        ));

        // Tạo roles
        Role userRole = createRole(PredefinedRole.USER_ROLE, "Basic user role", userPermissions);
        Role adminRole = createRole(PredefinedRole.ADMIN_ROLE, "Administrator role", adminPermissions);

        roleRepository.saveAll(Arrays.asList(userRole, adminRole));
        log.info("✅ Roles seeded successfully");
    }

    private void seedUsers() {
        log.info("👤 Seeding users...");
        
        // Tạo admin user
        Role adminRole = roleRepository.findById(PredefinedRole.ADMIN_ROLE)
            .orElseThrow(() -> new RuntimeException("Admin role not found"));

        User adminUser = createUser("admin", "admin", "Admin", "User", adminRole);
        userRepository.save(adminUser);
        
        log.warn("⚠️ Admin user created with username: admin, password: admin - PLEASE CHANGE IN PRODUCTION!");
        log.info("✅ Users seeded successfully");
    }

    // Helper methods
    private Permission createPermission(String name, String description) {
        Permission permission = new Permission();
        permission.setName(name);
        permission.setDescription(description);
        return permission;
    }

    private Role createRole(String name, String description, Set<Permission> permissions) {
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        role.setPermissions(permissions);
        return role;
    }

    private User createUser(String username, String password, String firstName, String lastName, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRoles(new HashSet<>(Arrays.asList(role)));
        return user;
    }

    private Set<Permission> getPermissionsByName(List<String> permissionNames) {
        return new HashSet<>(permissionRepository.findAllById(permissionNames));
    }
}
