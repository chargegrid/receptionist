(ns receptionist.core
  (:require [receptionist.routes :refer [app]]
            [receptionist.settings :refer [config]]
            [org.httpkit.server :refer [run-server]]
            [clojure.tools.logging :as log]
            [compojure.response :refer [Renderable render]]
            [perseverance.core :as p]
            [receptionist.persistence.db :as db])
  (:import (java.util.concurrent CompletableFuture))
  (:gen-class))

(extend-protocol Renderable
  CompletableFuture
  (render [future request] (render (.get future) request)))

(defn -main [& args]
  (let [port (or (:port config) 8075)]
    (log/info "Server running at port" port)
    (p/retry {} (db/attempt-migrate))
    (run-server app {:port port})))
