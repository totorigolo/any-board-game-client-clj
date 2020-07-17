(ns cards-client-clj.events
  (:require
   [clojure.test :as test]
   [cljs.spec.alpha :as s]
   [cljs.pprint :refer [pprint]]
   [ajax.core :as ajax]
   [day8.re-frame.http-fx]
   [re-frame.core :as re-frame]
   [cards-client-clj.db :as db]
   [cards-client-clj.wire.api :as wire-api]))

;; -- Interceptors -------------------------------------------------------------
;;

;; -- check-db interceptor -----------------------------------------------------
;;
;; Interceptor that checks the integrity of the application state. Must be added
;; to every event handler changing `app-db`.

(defn check-db-against-spec-or-throw
  "Throws an exception if `db` doesn't match the given spec."
  [spec db]
  (when-not (s/valid? spec db)
    (let [error-str (str "spec check failed: " (s/explain-str spec db))]
      (pprint error-str)
      (pprint db)
      (throw (ex-info error-str {})))))

;; Now we create an interceptor using `after` ...
(def check-db-interceptor (re-frame/after (partial check-db-against-spec-or-throw ::db/app-db)))

;; ... that we will rename for conciseness
(def check-db check-db-interceptor)

;; -- Helpers -----------------------------------------------------------------

(defn already-joined
  [db round-id]
  (let [joined-rounds (-> db :rounds :joined)]
    (contains? (keys joined-rounds) round-id)))

(defn notify-if-no-username-or-else
  [db no-username-notification else]
  (if (empty? (-> db :profile :username))
    {:dispatch [::new-notification no-username-notification]}
    else))

;; -- Effect Handlers ----------------------------------------------------------

(defonce websockets
  ^{:doc "Atom storing WebSocket JS objects."}
  (atom {}))

(defn str-json->kw-clj
  [str-json]
  (-> (.parse js/JSON str-json)
      (js->clj :keywordize-keys true)))

(defn kw-clj->str-json
  [clj]
  (->> clj
       (clj->js)
       (.stringify js/JSON)))

(defn start-websocket-monitoring
  [round-id player-id]
  (let [timeout-id (.setTimeout js/window
                                (fn []
                                  (re-frame/dispatch [::ws->ping round-id player-id])
                                  (start-websocket-monitoring round-id player-id))
                                60000)]
    (swap! websockets assoc-in [round-id player-id :ping-timeout-id] timeout-id)))

(re-frame/reg-fx
 ::join-round-websocket
 (fn [[round-id player-id]]
   (when (get websockets round-id)
     (println "WebSocket for round '" round-id "' already exists. Replacing."))
   (let [;; Build the WebSocket URL
         location (.-location js/window)
         protocol (case (.-protocol location)
                    "http:" "ws:"
                    "https:" "wss:")
         host (.-host location)
         url (str protocol "//" host "/api/round/" round-id "/join?playerId=" player-id)
         ;; Open the WebSocket
         websocket (js/WebSocket. url)]
     ;; Set the WebSocket callbacks
     (set! (.-onmessage websocket) #(re-frame/dispatch [::ws-received round-id (str-json->kw-clj (.-data %))]))
     (set! (.-onopen websocket) #(re-frame/dispatch [::ws-opened round-id (.-data %)]))
     (set! (.-onclose websocket) #(re-frame/dispatch [::ws-closed round-id (.-data %)]))
     (set! (.-onerror websocket) #(re-frame/dispatch [::ws-error round-id (.-data %)]))
     ;; Start regular ping
    ;;  (start-websocket-monitoring round-id player-id)
     ;; Store the WebSocket
     (swap! websockets assoc-in [round-id player-id :ws] websocket))))

(re-frame/reg-fx
 ::send-on-websocket
 (fn [[round-id player-id data]]
   (let [websocket (get-in @websockets [round-id player-id :ws])]
     (when (nil? websocket)
       (throw (js/Error. (str "No WebSocket for: " player-id "@" round-id))))
     (let [data (if (string? data) data
                    (kw-clj->str-json data))]
       (.send websocket data)))))


;; -- Event Handlers ----------------------------------------------------------

(re-frame/reg-event-fx
 ::ws-received
 [check-db]
 (fn [_ [_ round-id msg]]
   (case (:type msg)
     "PLAYER_CONNECTED" {:dispatch [::ws<-player-connected round-id msg]}
     "PLAYER_DISCONNECTED" {:dispatch [::ws<-player-disconnected round-id msg]}
     "GAME_STARTED" {:dispatch [::ws<-game-started round-id msg]}
     "CHAT" {:dispatch [::ws<-chat round-id msg]}
     "NOTIFICATION" {:dispatch [::ws<-notification round-id msg]}
     "COMPONENTS_UPDATES" {:dispatch [::ws<-components-update round-id msg]}
     "INTERFACE_UPDATE" {:dispatch [::ws<-interface-update round-id msg]}
     "ACTION_AWAITED" {:dispatch [::ws<-action-awaited round-id msg]}
     "PONG" {:dispatch [::ws<-pong round-id msg]}
     {})))

(re-frame/reg-event-db
 ::ws<-components-update
 [check-db]
 (fn [db [_ round-id {updates :components}]]
   (reduce
    (fn [db {:keys [type id component]}]
      (case type
        ("Create" "Update")
        (assoc-in db [:rounds :joined round-id :components id] (assoc component
                                                                      :id id))
        "Delete"
        (-> db
            (update-in [:rounds :joined round-id :components] dissoc id)
            (update-in [:rounds :joined round-id :interface] dissoc id))))
    db updates)))

(re-frame/reg-event-db
 ::ws<-interface-update
 [check-db]
 (fn [db [_ round-id {updates :components}]]
   (reduce
    (fn [db {:keys [id position]}]
      (assoc-in db [:rounds :joined round-id :interface id] {:component-id id
                                                             :position position}))
    db updates)))

(re-frame/reg-event-db
 ::ws<-action-awaited
 [check-db]
 (fn [db [_ round-id {actions :all_of}]]
   (reduce
    (fn [db {:keys [type target-component] :as action}]
      (update-in db [:rounds :joined round-id :actions] conj action))
    db actions)))

(re-frame/reg-event-fx
 ::ws<-player-connected
 [check-db]
 (fn [{db :db} [_ round-id {:keys [username message]}]]
   {:db (update-in db [:rounds :joined round-id :round-info/players] conj username)
    :dispatch [::new-notification #:notification{:title "A new player joined"
                                                 :text message}]}))

(re-frame/reg-event-fx
 ::ws<-player-disconnected
 [check-db]
 (fn [{db :db} [_ round-id {:keys [username]}]]
   {:db (update-in db [:rounds :joined round-id :round-info/players] disj username)
    :dispatch [::new-notification #:notification{:title "A player leaved the game"
                                                 :text (str username " leaved the game.")}]}))

(re-frame/reg-event-fx
 ::ws<-game-started
 [check-db]
 (fn [{db :db} [_ round-id _]]
   {:db (assoc-in db [:rounds :joined round-id :round-info/status] "started")
    :dispatch [::new-notification #:notification{:title "The game started!"
                                                 :text "The game started, be prepared to have fun!"}]}))

(re-frame/reg-event-fx
 ::ws<-chat
 [check-db]
 (fn [_ [_ _ msg]]
   {:dispatch [::new-notification #:notification{:title "New message in the chat"
                                                 :text (with-out-str (pprint msg))}]}))

(re-frame/reg-event-fx
 ::ws<-notification
 [check-db]
 (fn [_ [_ _ notification]]
   {:dispatch [::new-notification #:notification{:title "New notification from the game"
                                                 :text (with-out-str (pprint notification))}]}))

(re-frame/reg-event-fx
 ::ws->ping
 [check-db]
 (fn [_ [_ round-id player-id]]
   ;; TODO: Add intelligence about past responses, or absence thereof.
   {::send-on-websocket [round-id player-id {:type "PING"}]}))

(re-frame/reg-event-fx
 ::ws<-pong
 [check-db]
 (fn [_ [_ round-id player-id _]]
   (println "Received pong from server.")
   {}))

(re-frame/reg-event-fx
 ::ws->start-game
 [check-db]
 (fn [_ [_ round-id player-id]]
   {::send-on-websocket [round-id player-id {:type "START_GAME"}]}))

(re-frame/reg-event-fx
 ::ws->get-components
 [check-db]
 (fn [_ [_ round-id player-id]]
   {::send-on-websocket [round-id player-id {:type "GET_COMPONENTS"}]}))

(re-frame/reg-event-fx
 ::ws->awaited-actions
 [check-db]
 (fn [_ [_ round-id player-id]]
   {::send-on-websocket [round-id player-id {:type "AWAITED_ACTIONS"}]}))

(re-frame/reg-event-fx
 ::ws->action-response
 [check-db]
 (fn [_ [_ round-id player-id responses]]
   {::send-on-websocket [round-id player-id (merge {:type "ACTION_RESPONSE"}
                                                   responses)]}))

(re-frame/reg-event-fx
 ::ws->get-components
 [check-db]
 (fn [_ [_ round-id player-id ids]]
   {::send-on-websocket [round-id player-id {:type "GET_COMPONENTS" :ids ids}]}))

(re-frame/reg-event-fx
 ::start-game
 [check-db]
 (fn [_ [_ round-id player-id]]
   {:dispatch [::ws->start-game round-id player-id]}))

(re-frame/reg-event-fx
 ::refresh-board
 [check-db]
 (fn [{db :db} [_ round-id player-id]]
   {:dispatch-n [[::ws->get-components round-id player-id]
                 [::ws->awaited-actions round-id player-id]]
    :db (-> db
            (update-in [:rounds :joined round-id :actions] disj)
            (update-in [:rounds :joined round-id :components] disj)
            (update-in [:rounds :joined round-id :interface] disj))}))

(re-frame/reg-event-fx
 ::send-action-response
 [check-db]
 (fn [_ [_ round-id player-id responses]]
   {:dispatch [::ws->action-response round-id player-id responses]}))

(re-frame/reg-event-fx
 ::play-action
 [check-db]
 (fn [{db :db} [_ {:keys [source-id target-id]}]]
   (let [round-id (get-in db [:rounds :current])
         player-id (get-in db [:rounds :joined round-id :round-info/player-id])]
     {:dispatch-n [[::remove-action round-id source-id]
                   [::ws->action-response round-id player-id {source-id target-id}]]})))

(re-frame/reg-event-db
 ::remove-action
 [check-db]
 (fn [db [_ round-id target-component]]
   (update-in db [:rounds :joined round-id :actions]
              (fn [actions]
                (filter #(not= (:target_component %) target-component) actions)))))

(re-frame/reg-event-fx
 ::get-components-in-current-round
 [check-db]
 (fn [{db :db} [_ component-ids]]
   (let [round-id (get-in db [:rounds :current])
         player-id (get-in db [:rounds :joined round-id :round-info/player-id])]
     {:dispatch [::ws->get-components round-id player-id component-ids]})))

(re-frame/reg-event-db
 ::ws-opened
 [check-db]
 (fn [db [_ round-id]]
   (assoc-in db [:rounds :joined round-id :ws-state] :connected)))

(re-frame/reg-event-fx
 ::ws-closed
 [check-db]
 (fn [{db :db} [_ round-id]]
   {:db (assoc-in db [:rounds :joined round-id :ws-state] :closed)
    }))

(re-frame/reg-event-db
 ::ws-error
 [check-db]
 (fn [db [_ round-id]]
   db))

(re-frame/reg-event-db
 ::initialize-db
 [check-db]
 (fn [_ _]
   db/default-db))

(re-frame/reg-event-db
 ::set-active-page
 [check-db]
 (fn [db [_ active-page]]
   (assoc db :active-page active-page)))

(re-frame/reg-event-db
 ::set-re-pressed-example-text
 [check-db]
 (fn [db [_ value]]
   (assoc db :re-pressed-example-text value)))

(re-frame/reg-event-db
 ::edit-profile
 [check-db]
 (fn [db [_ editing?]]
   (assoc-in db [:profile :editing] editing?)))

(re-frame/reg-event-fx
 ::update-profile
 [check-db]
 (fn [{db :db} [_ {:keys [username]}]]
   {:db (-> db
            (assoc-in [:profile :username] username))
    :dispatch [::edit-profile false]}))

(re-frame/reg-event-db
 ::set-current-round
 [check-db]
 (fn [db [_ round-id]]
   (assoc-in db [:rounds :current] round-id)))

(re-frame/reg-event-fx
 ::join-round
 [check-db]
 (fn [{db :db} [_ round-id]]
   (notify-if-no-username-or-else
    db #:notification{:title "Cannot join round"
                      :text "You must choose a username for joining rounds."}
    (if (already-joined db round-id)
      {:dispatch [::set-current-round round-id]}
      {:http-xhrio {:method :get
                    :uri (str "/api/round/" round-id "/join")
                    :timeout 5000
                    :response-format (ajax/json-response-format {:keywords? true})
                    :params {:username (-> db :profile :username)}
                    :format          (ajax/json-request-format)
                    :on-success      [::join-round-success round-id]
                    :on-failure      [::join-round-failed round-id]}
       :db (assoc-in db [:rounds :joined round-id :joining?] true)}))))

(re-frame/reg-event-fx
 ::join-round-success
 [check-db]
 (fn
   [{db :db} [_ requested-round-id response]]
   (let [my-username (-> db :profile :username)
         {round-id :round-info/id
          player-id :round-info/player-id
          created-by :round-info/created-by
          :as round-information} (wire-api/RoundInformation->round-info response)]
     (test/is (= requested-round-id round-id))
     {:db (-> db
              (assoc-in [:rounds :joined round-id] (assoc round-information
                                                          :joining? false
                                                          :created-by-me? (= created-by my-username)
                                                          :actions #{})))
      :dispatch-n [[::set-active-page :play-page]
                   [::set-current-round round-id]
                   [::connect-round-ws round-id player-id]]})))

(re-frame/reg-event-fx
 ::join-round-failed
 [check-db]
 (fn [{db :db} [_ round-id response]]
   {:db (assoc-in db [:rounds :joined round-id :joining?] false)
    :dispatch [::new-notification #:notification{:title "Failed to join round"
                                                 :text (str "Failed to join round: " (-> response :response :message))}]}))

(re-frame/reg-event-fx
 ::create-round
 [check-db]
 (fn
   [{db :db} [_ gameId]]
   (notify-if-no-username-or-else
    db #:notification{:title "Cannot create round"
                      :text "You must choose a username for starting rounds."}
    {:http-xhrio {:method :post
                  :uri (str "/api/round/create/" gameId)
                  :timeout 5000
                  :response-format (ajax/json-response-format {:keywords? true})
                  :params {:username (-> db :profile :username)
                           :public true}
                  :format          (ajax/json-request-format)
                  :on-success      [::create-round-success]
                  :on-failure      [::create-round-failed]}
     :db (assoc-in db [:loading? :create-round] true)})))

(re-frame/reg-event-fx
 ::create-round-success
 [check-db]
 (fn
   [{db :db} [_ response]]
   (let [my-username (-> db :profile :username)
         {round-id :round-info/id
          player-id :round-info/player-id
          created-by :round-info/created-by
          :as round-information} (wire-api/RoundInformation->round-info response)]
   {:db (-> db
            (assoc-in [:loading? :create-round] false)
            (assoc-in [:rounds :joined round-id] (assoc round-information
                                                        :joining? false
                                                        :created-by-me? (= created-by my-username)
                                                        :actions #{})))
    :dispatch-n [[::set-active-page :play-page]
                 [::set-current-round round-id]
                 [::connect-round-ws round-id player-id]]})))

(re-frame/reg-event-fx
 ::connect-round-ws
 [check-db]
 (fn [{db :db} [_ round-id player-id]]
   (when-let [ws-state (get-in db [:rounds :joined round-id :ws-state])]
     (println "Round '' already has WebSocket in state: " ws-state ". It will be replaced."))
   {:db (assoc-in db [:rounds :joined round-id :ws-state] :connecting)
    ::join-round-websocket [round-id player-id]}))

(re-frame/reg-event-fx
 ::create-round-failed
 [check-db]
 (fn [{db :db} [_ response]]
   {:db (assoc-in db [:loading? :create-round] false)
    :dispatch [::new-notification #:notification{:title "Failed to start game"
                                                 :text (str "Failed to start game: " response)}]}))

(re-frame/reg-event-fx
 ::fetch-games
 [check-db]
 (fn
   [{db :db} _]
   {:http-xhrio {:method          :get
                 :uri             "/api/game/list"
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [::fetch-games-success]
                 :on-failure      [::fetch-games-failed]}
    :db (assoc-in db [:games :fetching?] true)}))

(re-frame/reg-event-db
 ::fetch-games-success
 [check-db]
 (fn [db [_ response]]
   (let [unique-games (distinct (map wire-api/GameDescription->game-description response))]
     (->
      db
      (assoc-in [:games :fetching?] false)
      (assoc-in [:games :game-list] unique-games)))))

(re-frame/reg-event-fx
 ::fetch-games-failed
 [check-db]
 (fn [{db :db} [_ response]]
   {:db (assoc-in db [:games :fetching?] false)
    :dispatch [::new-notification #:notification{:title "Failed to refresh games"
                                                 :text (str "Failed to refresh games: " response)}]}))

(re-frame/reg-event-fx
 ::fetch-rounds
 [check-db]
 (fn
   [{db :db} _]
   {:http-xhrio {:method          :get
                 :uri             "/api/round/list"
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [::fetch-rounds-success]
                 :on-failure      [::fetch-rounds-failed]}
    :db (assoc-in db [:loading? :rounds] true)}))

(re-frame/reg-event-db
 ::fetch-rounds-success
 [check-db]
 (fn [db [_ response]]
   (let [fetched-rounds (distinct (map wire-api/SimpleRoundInformation->round-info response))
         fetched-rounds-map (into {} (map (fn [{round-id :round-info/id :as round}]
                                            [round-id round])
                                          fetched-rounds))
         known-rounds (-> db :rounds :public-rounds)]
     (->
      db
      (assoc-in [:loading? :rounds] false)
      (assoc-in [:rounds :public-rounds]
                (merge ; Merge known rounds with public ones from the server, keeping server's if duplicates.
                 known-rounds
                 fetched-rounds-map))))))

(re-frame/reg-event-fx
 ::fetch-rounds-failed
 [check-db]
 (fn [{db :db} [_ response]]
   {:db (assoc-in db [:loading? :rounds] false)
    :dispatch [::new-notification #:notification{:title "Failed to refresh rounds"
                                                 :text (str "Failed to refresh rounds: " response)}]}))

(re-frame/reg-event-db
 ::new-notification
 [check-db]
 (fn [db [_ notification]]
   (let [created-at (-> js/Date .now)
         notification-id created-at
         notification (assoc notification
                             :notification/id notification-id
                             :notification/created-at created-at)]
     (assoc-in db [:notifications notification-id] notification))))

(re-frame/reg-event-db
 ::close-notification
 [check-db]
 (fn [db [_ notification-id]]
   (update-in db [:notifications] dissoc notification-id)))
