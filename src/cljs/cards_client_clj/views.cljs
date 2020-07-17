(ns cards-client-clj.views
  (:require
   [clojure.string]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as reagent]
   [cards-client-clj.subs :as subs]
   [cards-client-clj.events :as events]
   [cards-client-clj.routes :as routes]
   [cards-client-clj.views.pages.home :refer [home-page]]
   [cards-client-clj.views.pages.about :refer [about-page]]
   [cards-client-clj.views.pages.create-round :refer [create-round-page]]
   [cards-client-clj.views.pages.join-round :refer [join-round-page]]
   [cards-client-clj.views.pages.play :refer [play-page]]))


;; -- Navigation ---------------------------------------------------------------
;;

(defn nav
  []
  (reagent/with-let [active (reagent/atom false)]
    [:nav.navbar {:role "navigation" :aria-label "main navigation"}
     [:div.navbar-brand
      [:a.navbar-item {:href (routes/home)} "ABG"]
      [:a.navbar-burger.burger {:role "button"
                                :class (when @active "is-active")
                                :on-click #(reset! active (not @active))
                                :aria-label "menu"
                                :aria-expanded (if @active "true" "false")
                                :data-target "mainNavbar"}
       [:span {:aria-hidden true}]
       [:span {:aria-hidden true}]
       [:span {:aria-hidden true}]]]
     [:div#mainNavbar.navbar-menu {:class (when @active "is-active")}
      [:div.navbar-start
       [:a.navbar-item {:href (routes/create-round)} "Start a game"]
       [:a.navbar-item {:href (routes/join-round)} "Join a game"]
       [:div.navbar-item.has-dropdown.is-hoverable
        [:a.navbar-link "More"]
        [:div.navbar-dropdown
         [:a.navbar-item {:href (routes/about)} "About"]
         [:hr.navbar-divider]
         [:a.navbar-item "Report an issue"]]]]
      [:div.navbar-end
       [:div.navbar-item
        [:div.buttons
         [:a.button.is-primary {:href (routes/play)} "Play"]
         [:a.button.is-light {:on-click #(dispatch [::events/edit-profile true])}
          "Profile"]]]]]]))

;; -- Footer -------------------------------------------------------------------
;;

(defn footer []
  ;; [:footer.footer
  ;;  [:div.content.has-text-centered
  ;;   [:p "123"]]]
  nil)

;; -- Notifications ------------------------------------------------------------
;;

(defn view-notification
  [{id :notification/id title :notification/title text :notification/text}]
  [:div {:key id}
   [:h1 title]
   [:p text]
   [:button {:on-click #(dispatch [::events/close-notification id])} "X"]])

(defn notifications
  []
  (into [:<>]
        (let [notifications @(subscribe [::subs/notifications])]
          (for [notification (vals notifications)]
            ^{:key (:notification/id notification)}
            [view-notification notification]))))

;; -- Profile modal ------------------------------------------------------------
;;

(defn- profile-modal-inner
  "This inner component exists in order to get rid of temporary edits when the
   modal is closed. It has a inner state with the temporary edits."
  [{:keys [username]}]
  (reagent/with-let [new-username (reagent/atom username)]
    [:<>
     [:div.modal-background]
     [:div.modal-card
      [:header.modal-card-head
       [:p.modal-card-title "User profile"]
       [:button.delete {:aria-label "close" :on-click #(dispatch [::events/edit-profile false])}]]
      [:section.modal-card-body
       ;; TODO: Extract this code for a field to a function/namespace
       [:div.field.is-horizontal
        [:div.field-label.is-normal
         [:label.label "Username"]]
        [:div.field-body
         [:div.field
          [:p.control
           [:input.input {:type "text"
                          :placeholder "Username"
                          :value @new-username
                          :on-change #(reset! new-username (-> % .-target .-value))}]]]]]]
      [:footer.modal-card-foot
       [:button.button.is-success {:on-click #(dispatch [::events/update-profile {:username @new-username}])}
        "Save changes"]
       [:button.button {:on-click #(dispatch [::events/edit-profile false])} "Cancel"]]]]))

(defn profile-modal
  []
  (when @(subscribe [::subs/editing-profile])
    [:div.modal.is-active
     [profile-modal-inner {:username @(subscribe [::subs/username])}]]))

;; -- Root ---------------------------------------------------------------------
;;

(defn show-page [page-name]
  (case page-name
    :home-page [home-page]
    :about-page [about-page]
    :create-round-page [create-round-page]
    :join-round-page [join-round-page]
    :play-page [play-page]
    [:div (str "Not found: " page-name)]))

(defn root []
  [:div
   [profile-modal]
   [notifications]
   [nav]
   [:section.section
    [:div.container
     (let [active-page @(subscribe [::subs/active-page])]
       [show-page active-page])]]
   [footer]])
