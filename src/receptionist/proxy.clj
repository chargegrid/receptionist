(ns receptionist.proxy
  (:refer-clojure :exclude [proxy])
  (:require [clojure.tools.logging :as log]
            [clojure.string :refer [split]]
            [receptionist.settings :refer [config]]
            [ring.util.http-response :refer [internal-server-error]]
            [promesa.core :as p]
            [org.httpkit.client :as http]
            [clojure.walk :refer [stringify-keys]])
  (:import (java.net URL)))

(defn- build-url [host path query-string]
  (let [url (str (URL. (URL. host) path))]
    (if (not-empty query-string)
      (str url "?" query-string)
      url)))

(defn- identity-headers [headers {:keys [sub aud]}]
  (assoc headers "x-user-id" sub
                 "x-tenant-id" aud))

(defn request [opts]
  (p/promise (fn [resolve _] (http/request opts resolve))))

(defn- handle-error [response]
  (if-let [ex (:error response)]
    (do (log/error "Error in proxying request: " ex)
        (internal-server-error {:error "Could not proxy request"}))
    response))

(defn proxy-for [host-key]
  (fn [{:keys [headers identity uri query-string body request-method]}]
    (log/info "Proxying request" request-method uri)
    (let [stripped-headers (dissoc headers "content-length" "origin")
          full-headers (identity-headers stripped-headers identity)]
      (if-let [host (host-key (:proxy-hosts config))]
        (->> (request {:url     (build-url host uri query-string)
                       :method  (keyword request-method)
                       :body    body
                       :headers full-headers
                       :as      :stream})

             (p/map #(-> %
                         handle-error
                         (select-keys [:status :headers :body])
                         (update :headers dissoc :date :server)
                         (update :headers assoc :x-served-by (name host-key))
                         (update :headers stringify-keys))))

        (do (log/error "Proxy-host with key" host-key "does not exist")
            (internal-server-error))))))
