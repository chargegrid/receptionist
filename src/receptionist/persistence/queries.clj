(ns receptionist.persistence.queries
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "receptionist/persistence/sql/tenants.sql")
(hugsql/def-db-fns "receptionist/persistence/sql/users.sql")
(hugsql/def-db-fns "receptionist/persistence/sql/tokens.sql")
(hugsql/def-db-fns "receptionist/persistence/sql/roles.sql")
