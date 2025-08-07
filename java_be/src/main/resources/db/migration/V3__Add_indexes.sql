-- V3__Add_indexes.sql
-- Thêm indexes cho performance
CREATE INDEX idx_user_username ON user(username);
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_name ON user_roles(role_name);
CREATE INDEX idx_role_permissions_role_name ON role_permissions(role_name);
CREATE INDEX idx_role_permissions_permission_name ON role_permissions(permission_name);
CREATE INDEX idx_invalidated_token_expiry_time ON invalidated_token(expiry_time); 