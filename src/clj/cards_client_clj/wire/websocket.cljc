(ns cards-client-clj.wire.websocket
  (:require
   [clojure.spec.alpha :as s]
  ;;  [goog.json :as goog-json]
  ;;  [goog.json.Serializer]
   #?(:clj [clojure.test :refer [is]]
      :cljs [cljs.test :refer-macros [is]])))


;; -- HTTP API -----------------------------------------------------------------
;;
;; -- Spec ----------------------
;;


;; -- Conversion ----------------------
;;
(defn parse-ws-msg
  ""
  [msg]
  (prn msg)
  (condp = (get msg "type")
    "PLAYER_CONNECTED" 1
    "PING" 2
    "PONG" 2)
  msg)
