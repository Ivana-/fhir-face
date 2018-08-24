(ns fhir-face.views
  (:require
   [re-frame.core :as re-frame]
   [fhir-face.grid :as grid]
   [fhir-face.form :as form]))

#_(defn home-panel []
  (let [;;name (re-frame/subscribe [:subs/name])
        _ 33]
    [:div
     [:h1 (str "Hello from fhir-face. This is the Home Page.")]
     ;;[:button {:on-click (fn [] (re-frame/dispatch [::model/check-base-url]))} "Check"]
     ;;[:button {:on-click (fn [] (re-frame/dispatch [::cookies/set {:key "test" :value "test"}]))} "Set cookie"]
     ;;[:button {:on-click (fn [] (re-frame/dispatch [:set-cookie {:key "test" :value "test"}]))} "Set cookie"]
     ;;[:button {:on-click (fn [] (re-frame/dispatch [:remove-cookie "test"]))} "Remove cookie"]
     [:div [:a {:href "#/resource"} "go to Resource Page"]]
     [:br]
     [:div [:a {:href "#/about"} "go to About Page"]]]))

#_(defn about-panel []
  [:div
   [:h1 "This is the About Page."]
   [:div
    [:a {:href "#/"} "go to Home Page"]]])

(def routes (merge grid/routes form/routes))

(re-frame/reg-sub
 ::active-panel
 (fn [db _] (:active-panel db)))

(defn main-panel []
  (let [params @(re-frame/subscribe [::active-panel])]
    (if-let [route-fn (routes (:page params))]
      [route-fn params]
      [:div {:style {:font-size "30px"}} "No matched route"])))

