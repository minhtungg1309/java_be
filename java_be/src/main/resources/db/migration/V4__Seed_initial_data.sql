-- V4__Seed_initial_data.sql
-- Seeding permissions
INSERT INTO permission (name, description) VALUES
('USER_READ', 'Read user information'),
('USER_WRITE', 'Create/Update user'),
('USER_DELETE', 'Delete user'),
('ROLE_READ', 'Read roles'),
('ROLE_WRITE', 'Create/Update roles'),
('ROLE_DELETE', 'Delete roles'),
('PERMISSION_READ', 'Read permissions'),
('PERMISSION_WRITE', 'Create/Update permissions'),
('PERMISSION_DELETE', 'Delete permissions')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- Seeding roles
INSERT INTO role (name, description) VALUES
('USER', 'Basic user role'),
('ADMIN', 'Administrator role')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- Seeding role-permissions for USER role
INSERT INTO role_permissions (role_name, permission_name) VALUES
('USER', 'USER_READ')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- Seeding role-permissions for ADMIN role
INSERT INTO role_permissions (role_name, permission_name) VALUES
('ADMIN', 'USER_READ'),
('ADMIN', 'USER_WRITE'),
('ADMIN', 'USER_DELETE'),
('ADMIN', 'ROLE_READ'),
('ADMIN', 'ROLE_WRITE'),
('ADMIN', 'ROLE_DELETE'),
('ADMIN', 'PERMISSION_READ'),
('ADMIN', 'PERMISSION_WRITE'),
('ADMIN', 'PERMISSION_DELETE')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name); 