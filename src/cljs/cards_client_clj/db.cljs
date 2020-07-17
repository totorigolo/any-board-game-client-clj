(ns cards-client-clj.db
  (:require
   [cljs.spec.alpha :as s]))

;; -- Spec ---------------------------------------------------------------------
;;
;; This is a clojure.spec specification for the value in app-db. It is like a
;; Schema. See: http://clojure.org/guides/spec
;;
;; The value in app-db should always match this spec. Only event handlers
;; can change the value in app-db so, after each event handler
;; has run, we re-check app-db for correctness (compliance with the Schema).
;;
;; How is this done? Look in events.cljs and you'll notice that all handlers
;; have an "after" interceptor which does the spec re-check.
;;
;; None of this is strictly necessary. It could be omitted. But we find it
;; good practice.

;; (s/def ::id int?)
;; (s/def ::title string?)
;; (s/def ::done boolean?)
;; (s/def ::todo (s/keys :req-un [::id ::title ::done]))
;; (s/def ::todos (s/and
;;                 (s/map-of ::id ::todo)                     ;; in this map, each todo is keyed by its :id
;;                 #(instance? PersistentTreeMap %)           ;; is a sorted-map (not just a map)
;;                 ))
;; (s/def ::showing #{:all :active :done})

(s/def ::joining? boolean?)
(s/def ::fetching? boolean?)

(s/def :notification/id nat-int?)
(s/def :notification/title string?)
(s/def :notification/text string?)
(s/def :notification/created-at nat-int?)
(s/def ::notification (s/keys :req [:notification/id :notification/title :notification/text :notification/created-at]))
(s/def ::notifications (s/map-of :notification/id ::notification))

(s/def ::game-description (s/keys :req [:game-description/id
                            :game-description/rules
                            :game-description/description
                            :game-description/min-players
                            :game-description/max-players]))
(s/def ::game-list (s/coll-of ::game-description))
(s/def ::games (s/keys :req-un [::fetching? ::game-list]))

(s/def ::username (s/nilable string?))

(s/def ::active-page keyword?)


(s/def ::profile (s/keys :req-un []
                         :opt-un [::username]))

(s/def ::rounds (s/keys :req-un [::public-rounds ::joined ::current]))

(s/def ::app-db (s/keys :req-un [::games ::rounds ::profile ::notifications]
                        :opt-un [:breaking-point.core/breakpoints ::active-page]))

(comment
  (def example-app-db
    {:games {:fetching? true
             :game-list []}
     :rounds {:fetching? false
              :public-rounds {"b37458c069964a90acdd6d731b26851d" {:gameId "bataille"
                                                                  :id "b37458c069964a90acdd6d731b26851d"
                                                                  :started false
                                                                  :players ["toto" "toto2"]
                                                                  :createdOn "2020-06-28 14:20:57.029670"
                                                                  :createdBy "toto"}
                              "9b6279f9c0a14b5db8f75524248f959f" {:createdOn "2020-06-28 11:16:23.147761"
                                                                  :createdBy "toto"
                                                                  :minPlayers 2
                                                                  :public true
                                                                  :maxPlayers 4
                                                                  :playerId "427a1b3fda30425e96f21b4f2956d9fe"
                                                                  :status "pending"
                                                                  :id "9b6279f9c0a14b5db8f75524248f959f"
                                                                  :players ["toto" "toto2" "null"]
                                                                  :gameId "bataille"}}
              :joined {"9b6279f9c0a14b5db8f75524248f959f" {:id "9b6279f9c0a14b5db8f75524248f959f"
                                                           :player-id "427a1b3fda30425e96f21b4f2956d9fe"}}
              :current "9b6279f9c0a14b5db8f75524248f959f"}
     :profile {}
     :notifications {0 #:notification{:id 0
                                      :title "Alert"
                                      :text "I don't have inspiration"
                                      :created-at 123456}}
     :active-page :play-page
     :breaking-point.core/breakpoints {:screen-width 1709, :screen-height 492}})

  (s/valid? ::app-db example-app-db)
  (s/explain-str ::app-db example-app-db)

  (require '[cljs.spec.gen.alpha :as gen])

  (gen/sample (s/gen ::app-db))
  (gen/sample (s/gen ::notifications))
;
  )

;; -- Default app-db Value -----------------------------------------------------
;;
;; When the application first starts, this will be the value put in app-db.
;; Unless, of course, there are todos in the LocalStore (see further below)
;; Look in:
;;   1.  `core.cljs` for  "(dispatch-sync [:initialise-db])"
;;   2.  `events.cljs` for the registration of :initialise-db handler

(def default-db
  {:games {:fetching? false
           :game-list []}
   :rounds {:public-rounds {}
            :joined {}
            :current nil}
   :profile {:username nil
             :editing false}
   :notifications {}})
