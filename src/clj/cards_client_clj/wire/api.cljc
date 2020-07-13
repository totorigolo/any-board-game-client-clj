(ns cards-client-clj.wire.api
  (:require
   [clojure.spec.alpha :as s]
   #?(:clj [clojure.test :refer [is]]
      :cljs [cljs.test :refer-macros [is]])))


;; -- HTTP API -----------------------------------------------------------------
;;
;; -- Spec ----------------------
;;

(s/def :GameDescription/gameId string?)
(s/def :GameDescription/rules string?)
(s/def :GameDescription/description string?)
(s/def :GameDescription/min_players pos-int?)
(s/def :GameDescription/max_players pos-int?)
(s/def ::GameDescription (s/keys :req-un [:GameDescription/gameId
                                          :GameDescription/rules
                                          :GameDescription/description
                                          :GameDescription/min_players
                                          :GameDescription/max_players]))

(s/def :RoundInformation/id string?)
(s/def :RoundInformation/playerId string?)
(s/def :RoundInformation/gameId string?)
(s/def :RoundInformation/status #{"pending" "started"})
(s/def :RoundInformation/createdOn string?)
(s/def :RoundInformation/createdBy string?)
(s/def :RoundInformation/minPlayers pos-int?)
(s/def :RoundInformation/maxPlayers pos-int?)
(s/def :RoundInformation/public boolean?)
(s/def :RoundInformation/players (s/coll-of string?))
(s/def ::RoundInformation (s/keys :req-un [:RoundInformation/id
                                           :RoundInformation/playerId
                                           :RoundInformation/gameId
                                           :RoundInformation/status
                                           :RoundInformation/createdOn
                                           :RoundInformation/createdBy
                                           :RoundInformation/minPlayers
                                           :RoundInformation/maxPlayers
                                           :RoundInformation/public
                                           :RoundInformation/players]))

(s/def ::SimpleRoundInformation (s/keys :req-un [:RoundInformation/id
                                                 :RoundInformation/gameId
                                                 :RoundInformation/started
                                                 :RoundInformation/createdOn
                                                 :RoundInformation/createdBy
                                                 :RoundInformation/players]))

;; -- Conversion ----------------------
;;
(defn GameDescription->game-description
  "Converts a GameDescription from the server's API to the client format."
  {:test (fn []
           (is (= (GameDescription->game-description
                   {:gameId "game-name"
                    :rules "Some rules, long string."
                    :description "What is this game about?"
                    :min_players 2
                    :max_players 4})
                  #:game-description{:id "game-name"
                                     :rules "Some rules, long string."
                                     :description "What is this game about?"
                                     :min-players 2
                                     :max-players 4})))}
  [{:keys [gameId rules description min_players max_players] :as game-description}]
  (is (s/valid? ::GameDescription game-description)
      (str "Received GameDescription is not valid: " (s/explain-str ::GameDescription game-description)))
  #:game-description{:id gameId
                     :rules rules
                     :description description
                     :min-players min_players
                     :max-players max_players})

(defn RoundInformation->round-info
  "Converts a RoundInformation from the server's API to the client format."
  {:test (fn []
           (is (= (RoundInformation->round-info
                   {:id "123456789az"
                    :playerId "za987654321"
                    :gameId "game-name"
                    :status "pending"
                    :createdOn ""
                    :createdBy "foo"
                    :minPlayers 2
                    :maxPlayers 4
                    :public true
                    :players ["foo" "bar"]})
                  #:round-info{:id "123456789az"
                               :player-id "za987654321"
                               :game-id "game-name"
                               :status "pending"
                               :created-on ""
                               :created-by "foo"
                               :min-players 2
                               :max-players 4
                               :public true
                               :players #{"foo" "bar"}})))}
  [{:keys [id playerId gameId status createdOn createdBy minPlayers maxPlayers public players] :as round-information}]
  (is (s/valid? ::RoundInformation round-information)
      (str "Received RoundInformation is not valid: " (s/explain-str ::RoundInformation round-information)))
  #:round-info{:id id
               :game-id gameId
               :player-id playerId
               :status status
               :created-on createdOn
               :created-by createdBy
               :min-players minPlayers
               :max-players maxPlayers
               :public public
               :players (into #{} players)})

(defn SimpleRoundInformation->round-info
  "Converts a SimpleRoundInformation from the server's API to the client format."
  {:test (fn []
           (is (= (SimpleRoundInformation->round-info
                   {:id "123456789az"
                    :gameId "game-name"
                    :started false
                    :createdOn ""
                    :createdBy "foo"
                    :players ["foo" "bar"]})
                  #:round-info{:id "123456789az"
                               :game-id "game-name"
                               :started false
                               :created-on ""
                               :created-by "foo"
                               :players #{"foo" "bar"}})))}
  [{:keys [id gameId started createdOn createdBy players] :as round-information}]
  (is (s/valid? ::SimpleRoundInformation round-information)
      (str "Received SimpleRoundInformation is not valid: " (s/explain-str ::SimpleRoundInformation round-information)))
  #:round-info{:id id
               :game-id gameId
               :started started
               :created-on createdOn
               :created-by createdBy
               :players (into #{} players)})
