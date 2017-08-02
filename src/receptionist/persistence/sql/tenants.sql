-- Tenants queries

-- :name insert-tenant! :execute :affected
INSERT INTO tenants (id, name, name_key, description, default_operator_id, is_active)
VALUES (:id, :name, :name_key, :description, :default_operator_id, :is_active)
