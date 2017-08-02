(ns receptionist.routes
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.cors :refer [wrap-cors]]
            [receptionist.settings :refer [config]]
            [buddy.auth :refer [authenticated?]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.auth.accessrules :refer [error wrap-access-rules]]
            [ring.util.http-response :refer [ok not-found internal-server-error]]
            [receptionist.proxy :refer [proxy-for]]
            [receptionist.identity.auth :as auth]
            [receptionist.identity.routes :refer [user-routes]])
  (:import (java.util UUID)))

;; Authentication & Authorization

(def rules [{:uri            "/oauth/token"
             :handler        (constantly true)
             :request-method :post}
            {:uri     "/*"
             :handler authenticated?}])

(def auth-opt {:policy :reject :rules rules})

;; Proxy routes

(def tx-service-routes
  (let [proxy-fn (proxy-for :transaction-service)]
    (routes
      (ANY "/transactions*" [] proxy-fn)
      (GET "/evses/:id/transactions" [] proxy-fn))))

(def cs-routes
  (let [proxy-fn (proxy-for :central-system)]
    (routes
      (ANY "/summary" [] proxy-fn)
      (ANY "/sessions" [] proxy-fn)
      (ANY "/connections" [] proxy-fn)
      (ANY "/charge-boxes/:serial/sessions" [] proxy-fn)
      (ANY "/charge-boxes/:id/remote/*" [] proxy-fn)
      (ANY "/evses/:evse-id/remote/*" [] proxy-fn))))



(def log-routes
  (let [proxy-fn (proxy-for :log-service)]
    (routes
      (POST "/ocpp-logs" [] proxy-fn))))


(def pricing-routes
  (let [proxy-fn (proxy-for :pricing-service)]
    (routes
      (ANY "/pricing-*" [] proxy-fn)
      (GET "/evses/:id/pricing-policies" [] proxy-fn))))

(def charge-box-routes
  (let [proxy-fn (proxy-for :charge-box-service)]
    (routes
      (ANY "/groups*" [] proxy-fn)
      (ANY "/charge-boxes*" [] proxy-fn)
      (ANY "/evses*" [] proxy-fn))))


;; Everything put together

(defn add-served-by-header [handler]
  (fn [request]
    (let [response (handler request)]
      (if (get-in response [:headers "x-served-by"])
        response
        (update response :headers assoc "X-Served-By" "receptionist")))))

(def api-routes
  (api
    tx-service-routes
    cs-routes
    log-routes
    pricing-routes
    charge-box-routes
    user-routes))

(def app (-> #'api-routes
             (wrap-access-rules auth-opt)
             (wrap-authentication auth/backend)
             (wrap-authorization auth/backend)
             wrap-with-logger
             (wrap-cors :access-control-allow-origin (map re-pattern (:cors-urls config))
                        :access-control-allow-methods [:get :put :post :delete :options])
             add-served-by-header
             wrap-reload))
