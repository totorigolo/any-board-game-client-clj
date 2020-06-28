(ns cards-client-clj.db)

(def default-db
  {:name "Any Board Game"
   :games []
   :rounds {:known {}
            :joined {}
            :current nil}
   :profile {:username nil}
   :loading? {}
   :notifications {}})
