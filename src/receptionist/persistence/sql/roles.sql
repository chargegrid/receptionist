-- Roles queries

-- :name get-roles-for-tenant :query :many
SELECT
  r.*,
  STRING_AGG(pr.permission_id, ',') AS permissions
FROM roles r
  LEFT JOIN permissions_roles pr ON r.id = pr.role_id
WHERE tenant_id = :tenant_id
GROUP BY r.id;

-- :name find-role-by-id :query :one
SELECT
  r.*,
  STRING_AGG(pr.permission_id, ',') AS permissions
FROM roles r
  LEFT JOIN permissions_roles pr ON r.id = pr.role_id
WHERE id = :id AND tenant_id = :tenant_id
GROUP BY r.id;

-- :name insert-role! :returning-execute :raw
INSERT INTO roles (id, tenant_id, name, description)
VALUES (:id, :tenant_id, :name, :description)
RETURNING id;

-- :name insert-user-role! :execute :affected
INSERT INTO users_roles (user_id, role_id)
  SELECT
    users.id,
    roles.id
  FROM users
    JOIN users_tenants ON users.id = users_tenants.user_id
    JOIN tenants ON users_tenants.tenant_id = tenants.id
    JOIN roles ON tenants.id = roles.tenant_id
  WHERE
    users_tenants.tenant_id = :tenant_id AND
    users.id = :user_id
    AND roles.tenant_id = :tenant_id AND roles.id = :role_id
ON CONFLICT (user_id, role_id)
  DO NOTHING;

-- :name delete-user-role! :execute :affected
DELETE FROM users_roles ur
USING users_tenants ut, roles r
WHERE ut.user_id = ur.user_id AND r.id = ur.role_id AND ur.role_id = :role_id
      AND ur.user_id = :user_id AND ut.tenant_id = :tenant_id
      AND r.tenant_id = :tenant_id;
