(ns cards-client-clj.events
  (:require
   [clojure.test :as test]
   [cljs.spec.alpha :as s]
   [cljs.pprint :refer [pprint]]
   [ajax.core :as ajax]
   [day8.re-frame.http-fx]
   [re-frame.core :as re-frame]
   [cards-client-clj.db :as db]))

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

;; -- Event Handlers ----------------------------------------------------------

(re-frame/reg-event-db
 ::initialize-db
 [check-db]
 (fn [_ _]
   db/default-db))

(re-frame/reg-event-db
 ::set-active-panel
 [check-db]
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

(re-frame/reg-event-db
 ::set-re-pressed-example-text
 [check-db]
 (fn [db [_ value]]
   (assoc db :re-pressed-example-text value)))

(re-frame/reg-event-db
 ::change-username
 [check-db]
 (fn [db [_ username]]
   (assoc-in db [:profile :username] username)))

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
   (let [round-id (:id response)]
     (test/is (= requested-round-id round-id))
     {:db (-> db
              (assoc-in [:rounds :known round-id] response)
              (assoc-in [:rounds :joined round-id] {:id round-id
                                                    :player-id (:playerId response)
                                                    :joining? false}))
      :dispatch-n [[::set-active-panel :play-panel]
                   [::set-current-round round-id]]})))

(re-frame/reg-event-fx
 ::join-round-failed
 [check-db]
 (fn [{db :db} [_ round-id response]]
   {:db (assoc-in db [:rounds :joined round-id :joining?] false)
    :dispatch [::new-notification #:notification{:title "Failed to join round"
                                                 :text (str "Failed to join round: " (-> response :response :message))}]}))

(re-frame/reg-event-fx
 ::start-game
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
                  :on-success      [::start-game-success]
                  :on-failure      [::start-game-failed]}
     :db (assoc-in db [:loading? :start-game] true)})))

(re-frame/reg-event-fx
 ::start-game-success
 [check-db]
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
 [check-db]
 (fn [{db :db} [_ response]]
   {:db (assoc-in db [:loading? :start-game] false)
    :dispatch [::new-notification #:notification{:title "Failed to start game"
                                                 :text (str "Failed to start game: " response)}]}))

(re-frame/reg-event-fx
 ::refresh-games
 [check-db]
 (fn
   [{db :db} _]
   {:http-xhrio {:method          :get
                 :uri             "/api/game/list"
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [::refresh-games-success]
                 :on-failure      [::refresh-games-failed]}
    :db (assoc-in db [:games :fetching?] true)}))

(re-frame/reg-event-db
 ::refresh-games-success
 [check-db]
 (fn [db [_ response]]
   (->
    db
    (assoc-in [:games :fetching?] false)
    (assoc-in [:games :list] (distinct response)))))

(re-frame/reg-event-fx
 ::refresh-games-failed
 [check-db]
 (fn [{db :db} [_ response]]
   {:db (assoc-in db [:games :fetching?] false)
    :dispatch [::new-notification #:notification{:title "Failed to refresh games"
                                                 :text (str "Failed to refresh games: " response)}]}))

(re-frame/reg-event-fx
 ::refresh-rounds
 [check-db]
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
 [check-db]
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
