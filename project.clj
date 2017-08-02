(defproject receptionist "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 ;; HTTP
                 [http-kit "2.2.0"]
                 [metosin/compojure-api "1.1.10"]
                 [ring/ring-defaults "0.3.0"]
                 [ring/ring-json "0.4.0"]
                 [ring-cors "0.1.10"]
                 [ring/ring-devel "1.6.1"]
                 ;; security
                 [buddy/buddy-auth "1.4.1"]
                 [buddy/buddy-hashers "1.2.0"]
                 ;; data
                 [camel-snake-kebab "0.4.0"]
                 [org.clojure/data.json "0.2.6"]
                 ;; logging
                 [org.clojure/tools.logging "0.4.0"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [ring.middleware.logger "0.5.0" :exclusions [org.slf4j/slf4j-log4j12]]
                 ;; DB
                 [ragtime "0.7.1"]
                 [com.layerware/hugsql "0.4.7"]
                 [org.postgresql/postgresql "42.1.1"]
                 ;; misc
                 [com.grammarly/perseverance "0.1.2"]
                 [clj-time "0.13.0"]
                 [cprop "0.1.10"]
                 [funcool/promesa "1.8.1"]]

  :profiles {:uberjar {:aot :all}}

  :target-path "target/%s/"

  :main receptionist.core
  :uberjar-name "receptionist.jar")

