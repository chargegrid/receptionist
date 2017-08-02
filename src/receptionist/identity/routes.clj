(ns receptionist.identity.routes
  (:require [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [receptionist.identity.auth :as auth]
            [receptionist.identity.users :as users]
            [receptionist.identity.roles :as roles]
            [receptionist.identity.schema :as schema]
            [receptionist.compojure.restructures])
  (:import (java.util UUID)))

;; Routes that Receptionist likes to handle herself

(def user-routes
  (routes
    (POST "/oauth/token" []
      :form-params [grant_type :- s/Str
                    {username :- s/Str nil}
                    {password :- s/Str nil}
                    {tenant :- s/Str nil}
                    {refresh_token :- s/Str nil}]
      (auth/authenticate grant_type {:refresh-token refresh_token
                                     :tenant-key    tenant
                                     :username      username
                                     :password      password}))
    (GET "/me" []
      :current-user user-id
      :current-tenant tenant-id
      (users/get-user user-id tenant-id))
    (GET "/users" []
      :current-tenant tenant-id
      (users/list-users tenant-id))
    (GET "/users/search" []
      :query-params [email :- s/Str]
      (users/search-users email))
    (POST "/users" [req]
      :current-tenant tenant-id
      :require-permissions #{:create_user}
      :body [user schema/User]
      (users/create-user user tenant-id req))
    (GET "/users/:id" []
      :name :get-user
      :current-tenant tenant-id
      :path-params [id :- s/Uuid]
      (users/get-user id tenant-id))
    (DELETE "/users/:id" []
      :current-tenant tenant-id
      :path-params [id :- s/Uuid]
      (users/remove-user id tenant-id))
    (POST "/users/:id/roles/:role-id" []
      :current-tenant tenant-id
      :require-permissions #{:modify_user}
      :path-params [id :- s/Uuid role-id :- s/Uuid]
      (roles/add-role-to-user! id tenant-id role-id))
    (DELETE "/users/:id/roles/:role-id" []
      :current-tenant tenant-id
      :require-permissions #{:modify_user}
      :path-params [id :- s/Uuid role-id :- s/Uuid]
      (roles/remove-role-from-user! id tenant-id role-id))
    (GET "/roles" []
      :current-tenant tenant-id
      (roles/list-roles tenant-id))
    (GET "/roles/:id" []
      :name :get-role
      :current-tenant tenant-id
      :path-params [id :- s/Uuid]
      (roles/get-role id tenant-id))
    (POST "/roles" [:as req]
      :current-tenant tenant-id
      :require-permissions #{:create_role}
      :body [role schema/Role]
      (roles/create-role role tenant-id req))))
