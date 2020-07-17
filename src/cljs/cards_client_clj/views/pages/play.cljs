(ns cards-client-clj.views.pages.play
  (:require
   [clojure.string]
   [clojure.pprint :refer [pprint]]
   [re-frame.core :refer [subscribe dispatch]]
   [cards-client-clj.subs :as subs]
   [cards-client-clj.events :as events]))


(defn view-card-by-id
  [component-id]
  (let [component @(subscribe [::subs/current-round-component component-id])
        actions @(subscribe [::subs/current-round-component-action component-id])]
    (when (nil? component)
      ;; Request the component from the server if nil
      ;; TODO: Move this to the event handler
      (dispatch [::events/get-components-in-current-round [component-id]]))
    [:div.component {:style {:border "1px solid blue"}}
     (for [card (:cards component)]
       [:p (with-out-str (pprint card))])
     [:pre [:code (with-out-str (pprint component))]]
     [:pre [:code (with-out-str (pprint actions))]]]))

(defn view-card-deck
  [component actions]
  (let [on-click? (first (filter #(= (:type %) "OnClick") actions))]
    [:div.component {:style {:border "1px solid red"}}
     [:h4 (:id component)]
     (for [card-id (:cards component)]
       [:<> {:key card-id}
        [view-card-by-id card-id]
        (when on-click?
          [:button
           {:on-click #(dispatch [::events/play-action {:source-id (:id component)
                                                        :target-id card-id}])}
           "Play this card"])])
     [:pre [:code (with-out-str (pprint component))]]
     [:pre [:code (with-out-str (pprint actions))]]]))

(defn view-current-round-components
  [component-id]
  (let [{:keys [id type] :as component} @(subscribe [::subs/current-round-component component-id])
        actions @(subscribe [::subs/current-round-component-action component-id])]
    ^{:key id}
    (case type
      "CardDeck" (view-card-deck component actions)
      [:h3 (str "Unknown component type: " type)])))

(defn view-current-round-components-at-position
  [position]
  (for [component-id @(subscribe [::subs/current-round-components-id-at-position position])]
    ^{:key component-id} [view-current-round-components component-id]))

(defn play-round [{round-id :round-info/id
                   player-id :round-info/player-id
                   :as round}]
  [:div
   [:button {:on-click #(dispatch [::events/set-current-round nil])} "Change current round"]

   [:button {:on-click #(dispatch [::events/refresh-board round-id player-id])} "Refresh board"]

   (when (:created-by-me? round)
     [:button {:on-click #(dispatch [::events/start-game round-id player-id])} "Start the game"])
   [:p (str "Current round: " round-id)]

   [:h3 "center"]
   (view-current-round-components-at-position "center")

   [:h3 "top"]
   (view-current-round-components-at-position "top")

   [:h3 "left"]
   (view-current-round-components-at-position "left")

   [:h3 "right"]
   (view-current-round-components-at-position "right")

   [:h3 "bottom"]
   (view-current-round-components-at-position "bottom")])

(defn play-view-round [{:round-info/keys [id game-id player-id joining? players created-on]}]
  [:div
   [:h1 (str game-id " - " (clojure.string/join ", " players))]
   [:p (str "Joining: " (boolean joining?))]
   [:p (str "Round ID: " id)]
   [:p (str "Game ID: " game-id)]
   [:p (str "Player ID: " player-id)]
   [:p (str "Players: " players)]
   [:p (str "Created on: " created-on)]
   [:button {:on-click #(dispatch [::events/set-current-round id])}
    "Play"]])

(defn play-no-round []
  [:div
   [:p "Choose a round, or go join or create one."]
   (let [round-map @(subscribe [::subs/joined-rounds])]
     (if (empty? round-map) (str "You didn't join any round yet.")
         (for [{round-id :round-info/id :as round} (vals round-map)]
           ^{:key round-id} [play-view-round round])))])

(defn play-page []
  [:div
   (if-let [current-round @(subscribe [::subs/current-round])]
     [play-round current-round]
     [play-no-round])])
