-- V2__Create_junction_tables.sql
-- Tạo bảng user_roles (Many-to-Many relationship)
CREATE TABLE user_roles (
    user_id VARCHAR(36),
    role_name VARCHAR(50),
    PRIMARY KEY (user_id, role_name),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (role_name) REFERENCES role(name) ON DELETE CASCADE
);

-- Tạo bảng role_permissions (Many-to-Many relationship)
CREATE TABLE role_permissions (
    role_name VARCHAR(50),
    permission_name VARCHAR(50),
    PRIMARY KEY (role_name, permission_name),
    FOREIGN KEY (role_name) REFERENCES role(name) ON DELETE CASCADE,
    FOREIGN KEY (permission_name) REFERENCES permission(name) ON DELETE CASCADE
); 