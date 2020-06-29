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
 ::known-rounds
 :<- [::rounds]
 (fn [rounds _]
   (:known rounds)))

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
   (:list games)))

(re-frame/reg-sub
 ::joined-rounds
 :<- [::rounds]
 (fn [rounds _]
   (:joined rounds)))

(re-frame/reg-sub
 ::current-round
 :<- [::rounds]
 (fn [rounds _]
   (:current rounds)))

(re-frame/reg-sub
 ::username
 :<- [::profile]
 (fn [profile _]
   (:username profile)))

(re-frame/reg-sub
 ::known-round
 :<- [::known-rounds]
 (fn [known-rounds [_ round-id]]
   (get known-rounds round-id)))

(re-frame/reg-sub
 ::not-joined-known-rounds
 :<- [::known-rounds]
 :<- [::joined-rounds]
 (fn [[known-rounds joined-rounds] _]
   (filter ; Filter out rounds in joined-rounds
    #(not (joined-rounds (first %)))
    known-rounds)))

(re-frame/reg-sub
 ::notifications
 :<- [::unordered-notifications]
 (fn [unordered-notifications _]
   (sort unordered-notifications)))
