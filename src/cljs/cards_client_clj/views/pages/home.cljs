(ns cards-client-clj.views.pages.home
  (:require
   [re-frame.core :refer [subscribe]]
   [breaking-point.core :as bp]
   [cards-client-clj.subs :as subs]))


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

(defn home-page []
  [:div
   [display-re-pressed-example]
   [:div
    [:h3 (str "screen-width: " @(subscribe [::bp/screen-width]))]
    [:h3 (str "screen: " @(subscribe [::bp/screen]))]]])
