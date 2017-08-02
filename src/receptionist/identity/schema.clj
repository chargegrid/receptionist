(ns receptionist.identity.schema
  (:require [schema.core :as s]))

(s/defschema User
  {:email                      String
   :password                   String
   :first_name                 String
   :last_name                  String
   (s/optional-key :job_title) String})

(s/defschema Role
  {:name        String
   (s/optional-key :description) String})
