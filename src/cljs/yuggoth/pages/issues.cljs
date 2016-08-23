(ns yuggoth.pages.issues
  (:require [reagent.core :as r]
            [clojure.set :refer [difference rename-keys]]
            [re-frame.core :refer [dispatch subscribe]]
            [re-com.core
             :refer [box v-box h-split v-split title flex-child-style input-text input-textarea single-dropdown]]
            [re-com.splits
             :refer [hv-split-args-desc]]
            [yuggoth.bootstrap :as bs]
            [yuggoth.routes :refer [set-location!]]
            [clojure.string :as s]))

(def rounded-panel (flex-child-style "1"))

(def spacer [:span {:style {:margin-right "5px"}}])

(defn highlight-code [node]
  (let [nodes (.querySelectorAll (r/dom-node node) "pre code")]
    (loop [i (.-length nodes)]
      (when-not (neg? i)
        (when-let [item (.item nodes i)]
          (.highlightBlock js/hljs item))
        (recur (dec i))))))

(defn markdown-component []
  (r/create-class
    {:component-did-mount
     #(highlight-code (r/dom-node %))
     :component-did-update
     #(highlight-code (r/dom-node %))
     :reagent-render
     (fn [content]
       [:div.view-issue-detail
        {:dangerouslySetInnerHTML
         {:__html (-> content str js/marked)}}])}))

(defn preview-panel [text]
  [box
   :size "atuo"
   :class "edit-issue-detail"
   :child
   [:div.rounded-panel {:style rounded-panel}
    [markdown-component text]]])

(defn edit-panel [text]
  [box
   :size "atuo"
   :child
   [bs/FormControl
    {:component-class "textarea"
     :class           "edit-issue-detail"
     :placeholder     "issue detail"
     :value           @text
     :on-change       #(reset! text (-> % .-target .-value))}]])

(defn control-buttons [issue]
  (let [issue-id (:support-issue-id @issue)]
    [:div.row>div.col-sm-12
     [:div.pull-right
      [bs/Button
       {:bs-style "danger"
        :on-click #(set-location!
                    (if issue-id (str "#/issue/" issue-id) "#/"))}
       "Cancel"]
      spacer
      [bs/Button
       {:bs-style   "primary"
        :pull-right true
        :on-click   #(if issue-id
                      (dispatch [:save-issue @issue])
                      (dispatch [:create-issue @issue]))}
       "Save"]]]))

(defn render-tags [tags]
  [:span
   (for [tag tags]
     ^{:key tag}
     [bs/Label
      {:style {:margin-right "5px"}}
      tag])])

(defn tag-input [tags]
  (r/with-let [tags-text (r/atom (if-let [tags (not-empty @tags)]
                                   (s/join " " tags)
                                   ""))]
    [bs/FormControl
     {:type      "text"
      :value     @tags-text
      :on-change #(let [value (-> % .-target .-value)]
                   (reset! tags-text value)
                   (reset! tags (->> (s/split value #" ")
                                     (map s/trim)
                                     (set))))}]))

(defn tag-editor [tags]
  [:div.row
   [:div.col-sm-6
    [tag-input tags]]
   [:div.col-sm-6
    [:h4 [render-tags @tags]]]])

(defn edit-issue-page []
  (r/with-let [issue   (r/atom (-> @(subscribe [:issue])
                                   (update :title #(or % ""))
                                   (update :summary #(or % ""))
                                   (update :detail #(or % ""))
                                   (update :tags #(set (or % [])))))
               title   (r/cursor issue [:title])
               summary (r/cursor issue [:summary])
               detail  (r/cursor issue [:detail])
               tags    (r/cursor issue [:tags])]
    [v-box
     :size "auto"
     :gap "10px"
     :height "auto"
     :children
     [[control-buttons issue]
      [input-text
       :model title
       :class "edit-issue-title"
       :placeholder "title of the issue"
       :on-change #(reset! title %)]
      [input-text
       :model summary
       :width "100%"
       :placeholder "issue summary"
       :on-change #(reset! summary %)]
      [tag-editor tags]
      [h-split
       :panel-1 [edit-panel detail]
       :panel-2 [preview-panel @detail]
       :size "auto"]
      [control-buttons issue]]]))

(defn confirm-delete-modal [confirm-open? support-issue-id]
  [bs/Modal {:show @confirm-open?}
   [bs/Modal.Header
    [bs/Modal.Title "Are you sue you wish to delete the issue?"]]
   [bs/Modal.Body
    [bs/Button {:bs-style "danger"
                :on-click #(reset! confirm-open? false)}
     "Cancel"]
    spacer
    [bs/Button {:bs-style   "primary"
                :pull-right true
                :on-click   #(do
                              (reset! confirm-open? false)
                              (dispatch [:delete-issue support-issue-id]))}
     "Delete"]]])

(defn delete-issue [{:keys [support-issue-id]}]
  (r/with-let [confirm-open? (r/atom false)]
    [:div.pull-left
     [confirm-delete-modal confirm-open? support-issue-id]
     [bs/Button {:bs-style "danger"
                 :on-click #(reset! confirm-open? true)}
      "delete"]]))

(defn view-issue-page []
  (let [issue (subscribe [:issue])]
    [:div.row
     [:div.col-sm-12 [:h2 (:title @issue)]]
     [:div.col-sm-12 [:p (:summary @issue)]]
     [:div.col-sm-12 [:h4 "tags " (render-tags (:tags @issue))]]
     [:div.col-sm-12>hr]
     [:div.col-sm-12 [markdown-component (:detail @issue)]]
     [:div.col-sm-12
      [:div.pull-right
       [bs/FormGroup
        [delete-issue @issue]
        spacer
        [bs/Button
         {:bs-style   "primary"
          :pull-right true
          :on-click   #(set-location! "#/edit-issue")}
         "edit"]]]]]))

