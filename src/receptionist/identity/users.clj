(ns receptionist.identity.users
  (:require [receptionist.settings :refer [config]]
            [clojure.tools.logging :as log]
            [ring.util.http-response :refer [ok created not-found]]
            [receptionist.persistence.db :refer [db-spec-string] :rename {db-spec-string db}]
            [receptionist.persistence.queries :as q]
            [buddy.hashers :as hashers]
            [compojure.api.routes :as routes])
  (:import (java.util UUID)))

(def public-user-fields [:id :first_name :last_name :email :job_title])

(def input-user-fields (conj public-user-fields :password)) ;; We're not counting the tenant-id as a user field. It should never leak to the user.

(def empty-user (zipmap input-user-fields (repeat nil)))

(defn- get-roles [roles-with-user]
  (->> roles-with-user
       (filter :role_id)
       (map (fn [role-with-user]
              (let [{:keys [role_id role_name role_description]} role-with-user]
                {:id          role_id
                 :name        role_name
                 :description role_description})))))

(defn- group-users-with-roles [users-with-roles]
  (let [grouped (group-by #(select-keys % public-user-fields) users-with-roles)]
    (map (fn [[user roles-wtih-user]]
           (assoc user :roles (get-roles roles-wtih-user)))
         grouped)))

(defn get-user [id tenant-id]
  (let [user (q/find-user-with-roles-by-id db {:user_id id :tenant_id tenant-id})
        cleaned (group-users-with-roles user)]
    (if (seq cleaned)
      (ok (first cleaned))
      (not-found (str "User " id " not found!")))))

;; TODO: it feels like the request object shouldn't leak into this function, but it's needed for the `path-for*` function to work. How to fix?
(defn create-user [user tenant-id req]
  (let [id (UUID/randomUUID)
        hashed-pw (hashers/derive (:password user))
        user-for-db (merge empty-user (assoc user :id id :password hashed-pw))
        actual-id (:id (first (q/insert-user! db user-for-db)))]
    (q/insert-user-tenant! db {:user_id actual-id :tenant_id tenant-id})
    (if (= actual-id id)
      (let [uri (routes/path-for* :get-user req {:id id})]
        (created uri (dissoc user-for-db :password)))
      (let [actual-user (assoc user-for-db :id actual-id)]
        (ok (dissoc actual-user :password))))))

(defn list-users [tenant-id]
  (let [users (q/get-users-with-roles-for-tenant db {:tenant_id tenant-id})]
    (ok (group-users-with-roles users))))

(defn search-users [partial-email]
  (let [email-query (str partial-email "%")
        users (q/get-users-by-email db {:email_query email-query})
        cleaned (map #(select-keys % public-user-fields) users)]
    (ok cleaned)))

(defn remove-user [user-id tenant-id]
  (q/delete-user-tenant! db {:user_id user-id :tenant_id tenant-id}))

