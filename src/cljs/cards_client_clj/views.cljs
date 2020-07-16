(ns cards-client-clj.views
  (:require
   [clojure.string]
   [clojure.pprint :refer [pprint]]
   [re-frame.core :refer [subscribe dispatch]]
   [breaking-point.core :as bp]
   [cards-client-clj.subs :as subs]
   [cards-client-clj.events :as events]
   [cards-client-clj.routes :as routes]))

(defn nav []
  (for [{:keys [url title]} [{:url (routes/home) :title "Home"}
                             {:url (routes/about) :title "About"}
                             {:url (routes/create-round) :title "Create new round"}
                             {:url (routes/join-round) :title "Join round"}
                             {:url (routes/play) :title "Play"}]]
    [:div {:key title}
     [:a {:href url} title]]))

(defn view-username
  [{:keys [editable?]}]
  [:div
   [:label {:for "username"} "User name:"]
   [:input#username
    {:type "text"
     :disabled (not editable?)
     :value @(subscribe [::subs/username])
     :on-change #(dispatch [::events/change-username (-> % .-target .-value)])}]])

(defn view-notification
  [{id :notification/id title :notification/title text :notification/text}]
  [:div {:key id}
   [:h1 title]
   [:p text]
   [:button {:on-click #(dispatch [::events/close-notification id])} "X"]])

(defn panel
  [title body]
  [:div
   (let [notifications @(subscribe [::subs/notifications])]
     (for [notification (vals notifications)]
       ^{:key (:notification/id notification)} [view-notification notification]))
   [:h1 title]
   (nav)
   body])

(defn display-re-pressed-example []
  [:div
   [:p
    [:span "Listening for keydown events. A message will be displayed when you type "]
    [:strong [:code "hello"]]
    [:span ". So go ahead, try it out!"]]
   (when-let [re-pressed-example-text @(subscribe [::subs/re-pressed-example-text])]
     [:div
      {:style {:padding          "16px"
               :background-color "lightgrey"
               :border           "solid 1px grey"
               :border-radius    "4px"
               :margin-top       "16px"}}
      re-pressed-example-text])])

(defn home-panel []
  (panel "Home"
         [:div
          [display-re-pressed-example]
          [:div
           [:h3 (str "screen-width: " @(subscribe [::bp/screen-width]))]
           [:h3 (str "screen: " @(subscribe [::bp/screen]))]]]))

(defn about-panel []
  (panel "About"
         [:div
          [:p "Wonderbar website, WIP"]]))

(defn button-fetch-games
  []
  [:button {:class "button"
            :on-click  #(dispatch [::events/fetch-games])}
   "Refresh available games"])

(defn button-refresh-public-rounds
  []
  [:button {:class "button"
            :on-click  #(dispatch [::events/fetch-rounds])}
   "Refresh available rounds"])

(defn view-game
  [{:game-description/keys [id rules description min-players max-players]}]
  [:div
   [:h1 id]
   [:p description]
   [:p rules]
   [:p (str "Players: " min-players " - " max-players)]
   [:button {:on-click #(dispatch [::events/create-round id])}
    "Start"]])

(defn create-round-panel []
  (panel "Create round"
         [:div
          [button-fetch-games]
          [view-username {:editable? true}]
          (when @(subscribe [::subs/fetching-games?])
            [:p "Loading..."])
          (for [{game-id :game-description/id :as game} @(subscribe [::subs/game-list])]
            ^{:key game-id} [view-game game])]))

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

(defn play-panel []
  (panel "Play round"
         [:div
          [view-username {:editable? false}]
          (if-let [current-round @(subscribe [::subs/current-round])]
            [play-round current-round]
            [play-no-round])]))

(defn join-view-round [{:round-info/keys [id game-id players created-on]}]
  [:div
   [:h1 (str game-id " - " (clojure.string/join ", " players))]
   [:p (str "Round ID: " id)]
   [:p (str "Game ID: " game-id)]
   [:p (str "Players: " players)]
   [:p (str "Created on: " created-on)]
   [:button {:on-click #(dispatch [::events/join-round id])}
    "Join"]])

(defn join-round-panel []
  (panel "Join round"
         [:div
          [button-refresh-public-rounds]
          [view-username {:editable? true}]
          (when @(subscribe [::subs/fetching-rounds?])
            [:p "Fetching joinable rounds..."])
          (for [{round-id :round-info/id :as joinable-round} (vals @(subscribe [::subs/joinable-rounds]))]
            ^{:key round-id} [join-view-round joinable-round])]))

(defn- panels [panel-name]
  (case panel-name
    :home-panel [home-panel]
    :about-panel [about-panel]
    :create-round-panel [create-round-panel]
    :join-round-panel [join-round-panel]
    :play-panel [play-panel]
    [:div (str "Not found: " panel-name)]))

(defn show-panel [panel-name]
  [panels panel-name])

(defn root []
  [:div
   (let [active-panel @(subscribe [::subs/active-panel])]
     [show-panel active-panel])])
