(ns cards-client-clj.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:import [goog History]
           [goog.history EventType])
  (:require
   [secretary.core :as secretary]
   [goog.events :as gevents]
   [re-frame.core :as re-frame]
   [re-pressed.core :as rp]
   [cards-client-clj.events :as events]))

(defn hook-browser-navigation! []
  (doto (History.)
    (gevents/listen
     EventType/NAVIGATE
     (fn [^js event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn app-routes []
  (secretary/set-config! :prefix "#")
  ;; --------------------
  ;; define routes here
  (defroute home "/" []
    (re-frame/dispatch [::events/set-active-page :home-page])
    (re-frame/dispatch [::events/set-re-pressed-example-text nil])
    (re-frame/dispatch
     [::rp/set-keydown-rules
      {:event-keys [[[::events/set-re-pressed-example-text "Hi there!"]
                     [{:keyCode 72} ;; h
                      {:keyCode 69} ;; e
                      {:keyCode 76} ;; l
                      {:keyCode 76} ;; l
                      {:keyCode 79} ;; o
                      ]]]
       :clear-keys
       [[{:keyCode 27} ;; escape
         ]]}]))

  (defroute about "/about" []
    (re-frame/dispatch [::events/set-active-page :about-page]))

  (defroute create-round "/create-round" []
    (re-frame/dispatch [::events/set-active-page :create-round-page]))

  (defroute join-round "/join-round" []
    (re-frame/dispatch [::events/set-active-page :join-round-page]))

  (defroute play "/play" []
    (re-frame/dispatch [::events/set-active-page :play-page]))

  (defroute play-round "/play/:roundId" [round-id]
    (re-frame/dispatch [::events/set-current-round round-id])
    (re-frame/dispatch [::events/set-active-page :play-page]))


  ;; --------------------


  (hook-browser-navigation!))

