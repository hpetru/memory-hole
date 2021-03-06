(ns memory-hole.views
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [memory-hole.bootstrap :as bs]
            [memory-hole.routes :refer [context-url href navigate!]]
            [memory-hole.pages.common :refer [loading-throbber error-modal]]
            [memory-hole.pages.admin.groups :refer [groups-page]]
            [memory-hole.pages.admin.users :refer [users-page]]
            [memory-hole.pages.home :refer [home-page]]
            [memory-hole.pages.issues :refer [edit-issue-page view-issue-page]]
            [memory-hole.pages.auth :refer [login-page]]))

(defn nav-link [url title page]
  (let [active-page (subscribe [:active-page])]
    [bs/NavItem {:href (context-url url) :active (= page @active-page)} title]))

(defn navbar [{:keys [admin screenname]}]
  [bs/Navbar
   [bs/Navbar.Header]
   [bs/Navbar.Brand
    [:a#logo (href "/")
     [:span "Issues"]]]
   [bs/Navbar.Collapse
    (when (and admin (not js/ldap))
      [bs/Nav
       [nav-link "/users" "Manage Users" :users]])
    (when admin
      [bs/Nav
       [nav-link "/groups" "Manage Groups" :groups]])
    [bs/Nav {:pull-right true}
     [bs/NavDropdown
      {:id "logout-menu" :title screenname}
      [bs/MenuItem {:on-click #(dispatch [:logout])} "Logout"]]]]])

(defmulti pages (fn [page _] page))
(defmethod pages :home [_ _] [home-page])
(defmethod pages :login [_ _] [login-page])
(defmethod pages :users [_ user]
  (if (and (:admin user) (not js/ldap))
    [users-page]
    (navigate! "/")))
(defmethod pages :groups [_ user]
  (if (:admin user)
    [groups-page]
    (navigate! "/")))
(defmethod pages :edit-issue [_ _]
  (.scrollTo js/window 0 0)
  [edit-issue-page])
(defmethod pages :view-issue [_ _]
  (.scrollTo js/window 0 0)
  [view-issue-page])
(defmethod pages :default [_ _] [:div])

(defn main-page []
  (r/with-let [active-page (subscribe [:active-page])
               user        (subscribe [:user])]
    (if @user
      [:div
       [navbar @user]
       [loading-throbber]
       [error-modal]
       [:div.container.content
        (pages @active-page @user)]]
      (pages :login nil))))
