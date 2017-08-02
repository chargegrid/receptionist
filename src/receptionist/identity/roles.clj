(ns receptionist.identity.roles
  (:require [receptionist.persistence.db :refer [db-spec-string] :rename {db-spec-string db}]
            [receptionist.persistence.queries :as q]
            [ring.util.http-response :refer [ok created not-found no-content]]
            [compojure.api.routes :as routes])
  (:import (java.util UUID)))

(def public-role-fields [:id :name :description :permissions])

(def empty-role (zipmap public-role-fields (repeat nil)))

(defn- clean-role [role]
  (select-keys role public-role-fields))

(defn list-roles [tenant-id]
  (let [roles (q/get-roles-for-tenant db {:tenant_id tenant-id})
        cleaned (map clean-role roles)]
    (ok cleaned)))

(defn get-role [id tenant-id]
  (let [role (q/find-role-by-id db {:id id :tenant_id tenant-id})]
    (if role
      (ok (clean-role role))
      (not-found (str "Role " id " not found!")))))

(defn create-role [role tenant-id req]
  (let [id (UUID/randomUUID)
        role-for-db (merge empty-role (assoc role :id id :tenant_id tenant-id))
        uri (routes/path-for* :get-role req {:id id})]
    (q/insert-role! db role-for-db)
    (created uri (clean-role role-for-db))))

;; TODO: should we return an error when user tries to add an unknown role to a user?
(defn add-role-to-user! [user-id tenant-id role-id]
  (let [affected (q/insert-user-role! db {:user_id user-id :tenant_id tenant-id :role_id role-id})]
    (if (pos? affected)
      (created)
      (no-content))))

;; TODO: should we return an error when user tries to remove an unknown role?
;; What about an unknown user? Currently, it just returns "ok"
(defn remove-role-from-user! [user-id tenant-id role-id]
  (q/delete-user-role! db {:user_id user-id :tenant_id tenant-id :role_id role-id})
  (no-content))
