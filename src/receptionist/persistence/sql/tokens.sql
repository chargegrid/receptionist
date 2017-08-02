-- Refresh Tokens queries

-- :name insert-refresh-token! :execute :affected
INSERT INTO refresh_tokens (token, user_id, tenant_id, expires_at)
VALUES (:token, :user_id, :tenant_id, :expires_at)
ON CONFLICT (user_id, tenant_id) DO UPDATE
SET token = EXCLUDED.token, expires_at = EXCLUDED.expires_at;

-- :name delete-token! :execute :affected
DELETE FROM refresh_tokens WHERE token = :token;
