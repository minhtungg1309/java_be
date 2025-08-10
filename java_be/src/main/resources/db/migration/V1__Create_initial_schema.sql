-- V1__Create_initial_schema.sql
-- Tạo bảng user
CREATE TABLE user (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    dob DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Tạo bảng role
CREATE TABLE role (
    name VARCHAR(50) PRIMARY KEY,
    description VARCHAR(255)
);

-- Tạo bảng permission
CREATE TABLE permission (
    name VARCHAR(50) PRIMARY KEY,
    description VARCHAR(255)
);

-- Tạo bảng invalidated_token
CREATE TABLE invalidated_token (
    id VARCHAR(255) PRIMARY KEY,
    expiry_time DATETIME
); 