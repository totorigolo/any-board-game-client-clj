(ns cards-client-clj.views.pages.join-round
  (:require
   [clojure.string]
   [re-frame.core :refer [subscribe dispatch]]
   [cards-client-clj.subs :as subs]
   [cards-client-clj.events :as events]))


(defn join-view-round [{:round-info/keys [id game-id players created-on]}]
  [:div
   [:h1 (str game-id " - " (clojure.string/join ", " players))]
   [:p (str "Round ID: " id)]
   [:p (str "Game ID: " game-id)]
   [:p (str "Players: " players)]
   [:p (str "Created on: " created-on)]
   [:button {:on-click #(dispatch [::events/join-round id])}
    "Join"]])

(defn join-round-page []
  [:div
   [:button {:class "button"
             :on-click  #(dispatch [::events/fetch-rounds])}
    "Refresh available rounds"]
   (when @(subscribe [::subs/fetching-rounds?])
     [:p "Fetching joinable rounds..."])
   (for [{round-id :round-info/id :as joinable-round} (vals @(subscribe [::subs/joinable-rounds]))]
     ^{:key round-id} [join-view-round joinable-round])])
