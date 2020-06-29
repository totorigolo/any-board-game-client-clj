(ns cards-client-clj.views
  (:require
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

(defn button-refresh-games
  []
  [:button {:class "button"
            :on-click  #(dispatch [::events/refresh-games])}
   "Refresh available games"])

(defn button-refresh-known-rounds
  []
  [:button {:class "button"
            :on-click  #(dispatch [::events/refresh-rounds])}
   "Refresh available rounds"])

(defn view-game
  [{:keys [gameId rules description min_players max_players] :as game}]
  [:div
   [:h1 gameId]
   [:p description]
   [:p rules]
   [:p (str "Players: " min_players " - " max_players)]
   [:button {:on-click #(dispatch [::events/start-game gameId])}
    "Start"]])

(defn create-round-panel []
  (panel "Create round"
         [:div
          [button-refresh-games]
          [view-username {:editable? true}]
          (when @(subscribe [::subs/fetching-games?])
            [:p "Loading..."])
          (for [game @(subscribe [::subs/game-list])]
            ^{:key (:gameId game)} [view-game game])]))

(defn play-round [roundId]
  [:div
   [:button {:on-click #(dispatch [::events/set-current-round nil])}
    "Change current round"]
   [:p (str "Current round: " roundId)]])

(defn play-view-round [{:keys [id player-id joining?]}]
  (let [{:keys [gameId players createdOn]} @(subscribe [::subs/known-round id])]
    [:div {:key id}
     [:h1 id]
     [:p (str "Joining: " (boolean joining?))]
     [:p (str "Game ID: " gameId)]
     [:p (str "Player ID: " player-id)]
     [:p (str "Players: " players)]
     [:p (str "Created on: " createdOn)]
     [:button {:on-click #(dispatch [::events/set-current-round id])}
      "Play"]]))

(defn play-no-round []
  [:div
   [:p "Choose a round, or go join or create one."]
   (let [round-map @(subscribe [::subs/joined-rounds])]
     (if (empty? round-map) (str "You didn't join any round yet.")
         (for [round (vals round-map)]
           ^{:key (:id round)} [play-view-round round])))])

(defn play-panel []
  (panel "Play round"
         [:div
          [view-username {:editable? false}]
          (if-let [roundId @(subscribe [::subs/current-round])]
            [play-round roundId]
            [play-no-round])]))

(defn join-view-round [{:keys [id gameId players createdOn]}]
  [:div
   [:h1 id]
   [:p (str "Game ID: " gameId)]
   [:p (str "Players: " players)]
   [:p (str "Created on: " createdOn)]
   [:button {:on-click #(dispatch [::events/join-round id])}
    "Join"]])

(defn join-round-panel []
  (panel "Join round"
         [:div
          [button-refresh-known-rounds]
          [view-username {:editable? true}]
          (when @(subscribe [::subs/fetching-rounds?])
            [:p "Fetching joinable rounds..."])
          (for [joinable-round (vals @(subscribe [::subs/not-joined-known-rounds]))]
            ^{:key (:id joinable-round)} [join-view-round joinable-round])]))

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
