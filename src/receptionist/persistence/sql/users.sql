-- Users queries

-- :name insert-user! :returning-execute :raw
INSERT INTO users (id, first_name, last_name, email, password, job_title)
  VALUES (:id, :first_name, :last_name, :email, :password, :job_title)
ON CONFLICT (email) DO UPDATE -- We do this, instead of DO NOTHING, so we always get an ID back
SET email = EXCLUDED.email
RETURNING id;

-- :name insert-user-tenant! :execute :affected
INSERT INTO users_tenants (user_id, tenant_id)
  VALUES (:user_id, :tenant_id)
ON CONFLICT (user_id, tenant_id) DO NOTHING;

-- :name find-user-by-email-for-tenant :query :one
SELECT
  u.*,
  t.id                              AS tenant_id,
  STRING_AGG(pr.permission_id, ',') AS permissions
FROM users u
  JOIN users_tenants ut ON u.id = ut.user_id
  JOIN tenants t ON ut.tenant_id = t.id
  JOIN users_roles ur ON u.id = ur.user_id
  JOIN permissions_roles pr ON ur.role_id = pr.role_id
WHERE u.email = :email AND t.name_key = :tenant_key AND t.is_active = TRUE
GROUP BY u.id, t.id;

-- :name find-user-by-valid-token :query :one
SELECT
  u.*,
  t.id                              AS tenant_id,
  STRING_AGG(pr.permission_id, ',') AS permissions
FROM users u
  JOIN users_tenants ut ON u.id = ut.user_id
  JOIN tenants t ON ut.tenant_id = t.id
  JOIN refresh_tokens rt ON u.id = rt.user_id AND t.id = rt.tenant_id
  JOIN users_roles ur ON u.id = ur.user_id
  JOIN permissions_roles pr ON ur.role_id = pr.role_id
WHERE
  u.id = :user_id AND t.id = :tenant_id AND t.is_active = TRUE
  AND rt.token = :token
  AND rt.expires_at > now()
GROUP BY u.id, t.id;

-- :name find-user-with-roles-by-id :query :many
SELECT users.*, roles.id as role_id, roles.name as role_name, roles.description as role_description
FROM users
  JOIN users_tenants ON users.id = users_tenants.user_id
  JOIN tenants ON users_tenants.tenant_id = tenants.id
  LEFT JOIN users_roles ON users.id = users_roles.user_id
  LEFT JOIN roles ON tenants.id = roles.tenant_id AND roles.id = users_roles.role_id
WHERE users.id = :user_id AND tenants.id = :tenant_id AND tenants.is_active = true;

-- :name get-users-with-roles-for-tenant :query :many
SELECT users.*, roles.id as role_id, roles.name as role_name, roles.description as role_description
FROM users
  JOIN users_tenants ON users.id = users_tenants.user_id
  JOIN tenants ON users_tenants.tenant_id = tenants.id
  LEFT JOIN users_roles ON users.id = users_roles.user_id
  LEFT JOIN roles ON tenants.id = roles.tenant_id AND roles.id = users_roles.role_id
WHERE tenants.id = :tenant_id AND tenants.is_active = true;

-- :name get-users-by-email :query :many
SELECT *
FROM users
WHERE email LIKE :email_query;

-- :name delete-user-tenant! :execute :affected
DELETE FROM users_tenants
WHERE user_id = :user_id AND tenant_id = :tenant_id
