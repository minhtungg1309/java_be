
CREATE TABLE invalidated_token (
 id VARCHAR(255) PRIMARY KEY,
 expiry_time TIMESTAMP NOT NULL
);


CREATE TABLE permission (
 name VARCHAR(100) PRIMARY KEY,
 description VARCHAR(255)
);


CREATE TABLE role (
 name VARCHAR(100) PRIMARY KEY,
 description VARCHAR(255)
);

CREATE TABLE role_permissions (
role_name VARCHAR(100) NOT NULL,
permission_name VARCHAR(100) NOT NULL,
PRIMARY KEY (role_name, permission_name),
CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_name) REFERENCES role(name),
CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_name) REFERENCES permission(name)
);

CREATE TABLE user (
 id CHAR(36) PRIMARY KEY,
 username VARCHAR(50) NOT NULL UNIQUE,
 password VARCHAR(255) NOT NULL,
 first_name VARCHAR(100),
 last_name VARCHAR(100),
 dob DATE,
 avatar VARCHAR(255),
 city VARCHAR(100),
 email VARCHAR(100) UNIQUE,
 phone VARCHAR(20)
);

CREATE TABLE user_roles (
 user_id CHAR(36) NOT NULL,
 role_name VARCHAR(100) NOT NULL,
 PRIMARY KEY (user_id, role_name),
 CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES user(id),
 CONSTRAINT fk_user_roles_role FOREIGN KEY (role_name) REFERENCES role(name)
);
