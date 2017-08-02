(ns receptionist.compojure.restructures
  (:require [ring.util.http-response :refer [forbidden!]]
            [receptionist.identity.auth :refer [deserialize-permissions]])
  (:import (java.util UUID)))

(defmethod compojure.api.meta/restructure-param :current-user
  [_ binding acc]
  (update-in acc [:letks] into [binding `(UUID/fromString (:sub (:identity ~'+compojure-api-request+)))]))

(defmethod compojure.api.meta/restructure-param :current-tenant
  [_ binding acc]
  (update-in acc [:letks] into [binding `(UUID/fromString (:aud (:identity ~'+compojure-api-request+)))]))

(defn require-permissions! [required actual]
  (if-not (seq (clojure.set/intersection required actual))
    (forbidden! {:error "missing role" :required required :permissions actual})))

(defmethod compojure.api.meta/restructure-param :require-permissions [_ permissions acc]
  (update-in acc [:lets] into ['_ `(require-permissions! ~permissions (deserialize-permissions (:per (:identity ~'+compojure-api-request+))))]))
