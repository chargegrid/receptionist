(ns receptionist.identity.auth
  (:require [receptionist.settings :refer [config]]
            [clojure.tools.logging :as log]
            [buddy.sign.jwt :as jwt]
            [buddy.hashers :as hashers]
            [clojure.string :refer [split]]
            [receptionist.persistence.queries :as q]
            [receptionist.persistence.db :refer [db-spec-string] :rename {db-spec-string db}]
            [clj-time.core :as t]
            [clj-time.jdbc]
            [ring.util.http-response :refer [ok]]
            [buddy.auth.backends :as backends]
            [clojure.string :as str])
  (:import (clojure.lang ExceptionInfo)
           (java.util UUID)))

(defn auth-fn [authdata]
  (let [token-type (:type authdata)]
    (when (= token-type "access_token")
      authdata)))

(def backend (backends/jws {:secret     (get-in config [:auth :signing-secret])
                            :token-name "Bearer"
                            :authfn     auth-fn
                            :on-error   (fn [req e]
                                          (log/error "Error in jws-backend")
                                          (log/error (str e)))}))

(defn deserialize-permissions [permissions-str]
  (let [permissions (map keyword (str/split permissions-str #","))]
    (set permissions)))

(defn- serialize-permissions [permissions]
  (str/join "," (map name permissions)))

;; Create access token & refresh token in 1 operation, so issue times are the same
;; We add a :type to each token, so users can't use their refresh_token (which has a much longer TTL)
;; as Authorization header.
;; See https://stormpath.com/blog/fun-with-java-spring-boot-token-management#json-web-token-jwt-security-psa
(defn- create-tokens [user-id tenant-id permissions]
  (let [{:keys [access-token-expiration refresh-token-expiration issuer signing-secret]} (:auth config)
        now (t/now)
        permissions-str (serialize-permissions permissions)
        acc-exp (t/plus now (t/seconds access-token-expiration))
        ref-exp (t/plus now (t/seconds refresh-token-expiration))
        access-token (jwt/sign {:iat  now
                                :exp  acc-exp
                                :sub  user-id
                                :iss  issuer
                                :aud  tenant-id
                                :per  permissions-str
                                :type "access_token"} signing-secret)
        refresh-token (jwt/sign {:iat  now
                                 :exp  ref-exp
                                 :sub  user-id
                                 :iss  issuer
                                 :aud  tenant-id
                                 :type "refresh_token"} signing-secret)]
    {:access_token       access-token
     :refresh_token      refresh-token
     :token_type         "Bearer"
     :expires_in         access-token-expiration
     :refresh_expires_at ref-exp}))

(defn- issue-tokens [user]
  (let [{:keys [id tenant_id permissions]} user
        permissions-set (deserialize-permissions permissions)
        tokens (create-tokens id tenant_id permissions-set)
        {:keys [refresh_token refresh_expires_at]} tokens
        result (dissoc tokens :refresh_expires_at)]
    (q/insert-refresh-token! db {:token refresh_token :user_id id :tenant_id tenant_id :expires_at refresh_expires_at})
    (ok result)))

(defn- auth-error
  ([type]
   (auth-error type 400))
  ([type status-code]
   {:status  status-code
    :headers {}
    :body    {:error (name type)}}))

(defn- validate-token [token]
  (let [{:keys [issuer signing-secret]} (:auth config)]
    (try
      (jwt/unsign token signing-secret {:iss issuer})
      (catch ExceptionInfo e
        (log/info "Token validation failed: " (ex-data e))
        nil))))

(defn- get-user-for-token [token]
  (let [{:keys [sub aud]} (validate-token token)]
    (q/find-user-by-valid-token db {:user_id (UUID/fromString sub) :tenant_id (UUID/fromString aud) :token token})))

(defmulti authenticate (fn [grant_type _] (keyword grant_type)))

(defmethod authenticate :password [_ credentials]
  (if-not (every? credentials [:username :password :tenant-key])
    (auth-error :invalid_request)
    (let [{:keys [username password tenant-key]} credentials
          user (q/find-user-by-email-for-tenant db {:email username :tenant_key tenant-key})]
      (if (and user (hashers/check password (:password user)))
        (issue-tokens user)
        (auth-error :invalid_grant 401)))))

(defmethod authenticate :refresh_token [_ credentials]
  (if-not (:refresh-token credentials)
    (auth-error :invalid_request)
    (let [refresh-token (:refresh-token credentials)
          user (get-user-for-token refresh-token)]
      (if user
        (issue-tokens user)
        (do
          (q/delete-token! db {:token refresh-token})
          (auth-error :invalid_grant 401))))))

(defmethod authenticate :default [& _]
  (auth-error :unsupported_grant_type))
