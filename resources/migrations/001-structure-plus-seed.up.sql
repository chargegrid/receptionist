CREATE TABLE IF NOT EXISTS tenants (
  id                  UUID,
  name                VARCHAR(255) NOT NULL CHECK (length(name) > 0),
  name_key            VARCHAR(50)  NOT NULL CHECK (length(name_key) > 0),
  description         TEXT,
  default_operator_id VARCHAR(3),
  is_active           BOOLEAN      NOT NULL,
  PRIMARY KEY (id)
);
CREATE INDEX tenants_name_key_is_active
  ON tenants (name_key, is_active);

--;;

CREATE TABLE IF NOT EXISTS users (
  id         UUID,
  first_name VARCHAR(255) NOT NULL CHECK (length(first_name) > 0),
  last_name  VARCHAR(255) NOT NULL CHECK (length(last_name) > 0),
  email      VARCHAR(255) NOT NULL CHECK (length(email) > 0),
  password   VARCHAR(255) NOT NULL CHECK (length(password) > 0),
  job_title  VARCHAR(255),
  PRIMARY KEY (id)
);
CREATE UNIQUE INDEX users_email
  ON users (email);

--;;

CREATE TABLE users_tenants (
  user_id   UUID REFERENCES users,
  tenant_id UUID REFERENCES tenants,
  PRIMARY KEY (user_id, tenant_id)
);
CREATE INDEX users_tenants_tenant_id
  ON users_tenants (tenant_id);

--;;

CREATE TABLE roles (
  id          UUID,
  tenant_id   UUID         NOT NULL REFERENCES tenants,
  name        VARCHAR(255) NOT NULL CHECK (length(name) > 0),
  description TEXT,
  PRIMARY KEY (id)
);
CREATE INDEX roles_tenant_id
  ON roles (tenant_id);

--;;

CREATE TABLE users_roles (
  user_id UUID REFERENCES users,
  role_id UUID REFERENCES roles,
  PRIMARY KEY (user_id, role_id)
);
CREATE INDEX users_roles_user_id
  ON users_roles (user_id);

--;;

CREATE TABLE permissions (
  id          VARCHAR(50),
  name        VARCHAR(255) NOT NULL CHECK (length(name) > 0),
  description TEXT,
  PRIMARY KEY (id)
);

INSERT INTO permissions (id, name, description) VALUES
  ('view_tx', 'View Transactions', 'View all transactions'),
  ('view_cbs', 'View Charge Boxes', 'View all charge boxes'),
  ('register_cb', 'Register Charge Box', 'Register a currently unregistered charge box'),
  ('view_policies', 'View Pricing Policies', 'View all pricing policies and their pricing rules'),
  ('add_policy', 'Add Pricing Policy', 'Create a new pricing policy with optionally one or more pricing rules'),
  ('view_users', 'View Users', 'View all users'),
  ('create_user', 'Create User', 'Create a user'),
  ('modify_user', 'Modify User', 'Modify a user by adding and/or removing roles'),
  ('remove_user', 'Remove User', 'Remove user so it cannot login anymore'),
  ('view_roles', 'View Roles', 'View all available roles'),
  ('create_role', 'Create Role', 'Create a new role and optionally grant permissions to this role'),
  ('modify_role', 'Modify Role', 'Modify a role by granting/revoking permissions');

--;;

CREATE TABLE permissions_roles (
  permission_id VARCHAR(50) REFERENCES permissions,
  role_id       UUID REFERENCES roles,
  PRIMARY KEY (permission_id, role_id)
);
CREATE INDEX permissions_roles_role_id
  ON permissions_roles (role_id);

--;;

CREATE TABLE refresh_tokens (
  token TEXT CHECK (length(token) > 0),
  user_id UUID NOT NULL REFERENCES users,
  tenant_id UUID NOT NULL REFERENCES tenants,
  expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
  PRIMARY KEY (token)
);
CREATE UNIQUE INDEX refresh_tokens_user_tenant_unique
  ON refresh_tokens (user_id, tenant_id);
