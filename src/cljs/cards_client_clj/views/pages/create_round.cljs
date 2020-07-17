(ns cards-client-clj.views.pages.create-round
  (:require
   [clojure.string]
   [re-frame.core :refer [subscribe dispatch]]
   [cards-client-clj.subs :as subs]
   [cards-client-clj.events :as events]))


(defn view-game
  [{:game-description/keys [id rules description min-players max-players]}]
  [:div
   [:h1 id]
   [:p description]
   [:p rules]
   [:p (str "Players: " min-players " - " max-players)]
   [:button {:on-click #(dispatch [::events/create-round id])}
    "Start"]])

(defn create-round-page []
  [:div
   [:button {:class "button"
             :on-click  #(dispatch [::events/fetch-games])}
    "Refresh available games"]
   (when @(subscribe [::subs/fetching-games?])
     [:p "Loading..."])
   (for [{game-id :game-description/id :as game} @(subscribe [::subs/game-list])]
     ^{:key game-id} [view-game game])])
