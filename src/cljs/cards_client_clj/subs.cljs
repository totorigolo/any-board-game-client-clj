(ns cards-client-clj.subs
  (:require
   [re-frame.core :as re-frame]))

;; -------------------------------------------------------------------------------------
;; For documentation about layers, see:
;;   https://github.com/day8/re-frame/blob/master/docs/SubscriptionInfographic.md

;; -------------------------------------------------------------------------------------
;; Layer 2 - Extractors

(re-frame/reg-sub
 ::active-panel
 (fn [db _]
   (:active-panel db)))

(re-frame/reg-sub
 ::rounds
 (fn [db _]
   (:rounds db)))

(re-frame/reg-sub
 ::games
 (fn [db _]
   (:games db)))

(re-frame/reg-sub
 ::re-pressed-example-text
 (fn [db _]
   (:re-pressed-example-text db)))

(re-frame/reg-sub
 ::unordered-notifications
 (fn [db _]
   (:notifications db)))

(re-frame/reg-sub
 ::profile
 (fn [db _]
   (:profile db)))

;; -------------------------------------------------------------------------------------
;; Layer 3 - Materialized views

(re-frame/reg-sub
 ::fetching-rounds?
 :<- [::rounds]
 (fn [rounds _]
   (:fetching? rounds)))

(re-frame/reg-sub
 ::fetching-games?
 :<- [::games]
 (fn [games _]
   (:fetching? games)))

(re-frame/reg-sub
 ::game-list
 :<- [::games]
 (fn [games _]
   (:game-list games)))

(re-frame/reg-sub
 ::joined-rounds
 :<- [::rounds]
 (fn [rounds _]
   (:joined rounds)))

(re-frame/reg-sub
 ::current-round-id
 :<- [::rounds]
 (fn [rounds _]
   (:current rounds)))

(re-frame/reg-sub
 ::current-player-id
 :<- [::current-round]
 (fn [current-round  _]
   (:round-info/player-id current-round)))

(re-frame/reg-sub
 ::current-round
 :<- [::rounds]
 :<- [::current-round-id]
 (fn [[rounds id] _]
   (get-in rounds [:joined id])))

(re-frame/reg-sub
 ::current-round-components
 :<- [::current-round]
 (fn [current-round _]
   (get-in current-round [:components])))

(re-frame/reg-sub
 ::current-round-component
 :<- [::current-round-components]
 (fn [components [_ component-id]]
   (get components component-id)))

(re-frame/reg-sub
 ::current-round-actions
 :<- [::current-round]
 (fn [current-round _]
   (get-in current-round [:actions])))

(re-frame/reg-sub
 ::current-round-component-action
 :<- [::current-round-actions]
 (fn [actions [_ component-id]]
   (filter (fn [{id :target_component}] (= id component-id)) actions)))

(re-frame/reg-sub
 ::current-round-interface
 :<- [::current-round]
 (fn [current-round _]
   (get-in current-round [:interface])))

(re-frame/reg-sub
 ::current-round-components-id-at-position
 :<- [::current-round-interface]
 (fn [interface [_ position]]
   (->> interface
        (filter (fn [[_ info]] (= (:position info) position)))
        (map #(:component-id (second %))))))

(re-frame/reg-sub
 ::username
 :<- [::profile]
 (fn [profile _]
   (:username profile)))

(re-frame/reg-sub
 ::public-rounds
 :<- [::rounds]
 (fn [rounds _]
   (:public-rounds rounds)))

(re-frame/reg-sub
 ::joinable-rounds
 :<- [::public-rounds]
 :<- [::username]
 (fn [[public-rounds username] _]
   (filter ; Filter out rounds where current username is taken
    (fn [[_round-id {:round-info/keys [players]}]]
      (not (contains? players username)))
    public-rounds)))

(re-frame/reg-sub
 ::notifications
 :<- [::unordered-notifications]
 (fn [unordered-notifications _]
   (sort unordered-notifications)))
