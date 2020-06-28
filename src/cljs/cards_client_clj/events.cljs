(ns cards-client-clj.events
  (:require
   [ajax.core :as ajax]
   [day8.re-frame.http-fx]
   [re-frame.core :as re-frame]
   [cards-client-clj.db :as db]))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

(re-frame/reg-event-db
 ::set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

(re-frame/reg-event-db
 ::set-re-pressed-example
 (fn [db [_ value]]
   (assoc db :re-pressed-example-text value)))

(re-frame/reg-event-db
 ::change-username
 (fn [db [_ username]]
   (assoc-in db [:profile :username] username)))


(re-frame/reg-event-db
 ::set-current-round
 (fn [db [_ round-id]]
   (assoc-in db [:rounds :current] round-id)))

(defn already-joined
  [db round-id]
  (let [joined-rounds (-> db :rounds :joined)]
    (contains? (keys joined-rounds) round-id)))

(re-frame/reg-event-fx
 ::join-round
 (fn [{db :db} [_ round-id]]
   (if (already-joined db round-id)
     {:dispatch [::set-current-round round-id]}
     {:http-xhrio {:method :get
                   :uri (str "/api/round/" round-id "/join")
                   :timeout 5000
                   :response-format (ajax/json-response-format {:keywords? true})
                   :params {:username (-> db :profile :username)}
                   :format          (ajax/json-request-format)
                   :on-success      [::join-round-success]
                   :on-failure      [::join-round-failed]}
      :db (assoc-in db [:loading? :join-round] true)})))

(re-frame/reg-event-fx
 ::join-round-success
 (fn
   [{db :db} [_ response]]
   (let [round-id (:id response)]
     {:db (-> db
              (assoc-in [:loading? :join-round] false)
              (assoc-in [:rounds :known round-id] response)
              (assoc-in [:rounds :joined round-id] {:id round-id :player-id (:playerId response)}))
      :dispatch-n [[::set-active-panel :play-panel]
                   [::set-current-round round-id]]})))

(re-frame/reg-event-fx
 ::join-round-failed
 (fn [{db :db} [_ response]]
   {:db (assoc-in db [:loading? :join-round] false)
    :dispatch [::new-notification {:title "Failed to join round"
                                   :text (str "Failed to join round: " (-> response :response :message))}]}))

(re-frame/reg-event-fx
 ::start-game
 (fn
   [{db :db} [_ gameId]]
   {:http-xhrio {:method :post
                 :uri (str "/api/round/create/" gameId)
                 :timeout 5000
                 :response-format (ajax/json-response-format {:keywords? true})
                 :params {:username (-> db :profile :username)
                          :public true}
                 :format          (ajax/json-request-format)
                 :on-success      [::start-game-success]
                 :on-failure      [::start-game-failed]}
    :db (assoc-in db [:loading? :start-game] true)}))

(re-frame/reg-event-fx
 ::start-game-success
 (fn
   [{db :db} [_ response]]
   {:db (let [round-id (:id response)]
          (->
           db
           (assoc-in [:rounds :current] round-id)
           (assoc-in [:loading? :start-game] false)
           (assoc-in [:rounds :known round-id] response)
           (assoc-in [:rounds :joined round-id] {:id round-id :player-id (:playerId response)})))
    :dispatch [::set-active-panel :play-panel]}))

(re-frame/reg-event-fx
 ::start-game-failed
 (fn [{db :db} [_ response]]
   {:db (assoc-in db [:loading? :start-game] false)
    :dispatch [::new-notification {:title "Failed to start game"
                                   :text (str "Failed to start game: " response)}]}))

(re-frame/reg-event-fx
 ::refresh-games
 (fn
   [{db :db} _]
   {:http-xhrio {:method          :get
                 :uri             "/api/game/list"
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [::refresh-games-success]
                 :on-failure      [::refresh-games-failed]}
    :db (assoc-in db [:loading? :games] true)}))

(re-frame/reg-event-db
 ::refresh-games-success
 (fn [db [_ response]]
   (->
    db
    (assoc-in [:loading? :games] false)
    (assoc :games (distinct response)))))

(re-frame/reg-event-fx
 ::refresh-games-failed
 (fn [{db :db} [_ response]]
   {:db (assoc-in db [:loading? :games] false)
    :dispatch [::new-notification {:title "Failed to refresh games"
                                   :text (str "Failed to refresh games: " response)}]}))

(re-frame/reg-event-fx
 ::refresh-rounds
 (fn
   [{db :db} _]
   {:http-xhrio {:method          :get
                 :uri             "/api/round/list"
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [::refresh-rounds-success]
                 :on-failure      [::refresh-rounds-failed]}
    :db (assoc-in db [:loading? :rounds] true)}))

(re-frame/reg-event-db
 ::refresh-rounds-success
 (fn [db [_ response]]
   (->
    db
    (assoc-in [:loading? :rounds] false)
    (assoc-in [:rounds :known]
              (merge ; Merge known rounds with public ones from the server, keeping ours if duplicates.
               (into {} (map (fn [round] [(:id round) round]) (distinct response)))
               (-> db :rounds :known))))))

(re-frame/reg-event-fx
 ::refresh-rounds-failed
 (fn [{db :db} [_ response]]
   {:db (assoc-in db [:loading? :rounds] false)
    :dispatch [::new-notification {:title "Failed to refresh rounds"
                                   :text (str "Failed to refresh rounds: " response)}]}))

(re-frame/reg-event-db
 ::new-notification
 (fn [db [_ notification]]
   (let [notification-id (-> db :notifications count)
         notification (assoc notification :id notification-id)]
     (assoc-in db [:notifications notification-id] notification))))

(re-frame/reg-event-db
 ::close-notification
 (fn [db [_ notification-id]]
   (update-in db [:notifications] dissoc notification-id)))
