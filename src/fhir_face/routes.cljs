(ns fhir-face.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:import goog.History)
  (:require
   [secretary.core :as secretary]
   [goog.events :as gevents]
   [goog.history.EventType :as EventType]
   [re-frame.core :as re-frame]
   [fhir-face.model :as model]))

(re-frame/reg-event-db
 ::set-active-panel
 (fn [db [_ active-panel]]
   (-> db
       (assoc :active-panel active-panel)
       (assoc-in (conj model/root-path :data :is-fetching) true))))

(defn hook-browser-navigation! []
  (doto (History.)
    (gevents/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn app-routes []
  (secretary/set-config! :prefix "#")
  ;; --------------------
  ;; define routes here
  ;; https://github.com/gf3/secretary
  #_(defroute "/" []
    (re-frame/dispatch [::set-active-panel :home-panel]))

  #_(defroute "/about" []
    (re-frame/dispatch [::set-active-panel (merge params {:page :about-panel})]))

  (defroute "/resource" {:as params}
    (re-frame/dispatch [::set-active-panel (merge params {:page :resource-grid})]))

  (defroute "/resource/new" {:as params}
    (re-frame/dispatch [::set-active-panel (merge params {:page :resource-new})]))

  (defroute "/resource/edit" {:as params}
    (re-frame/dispatch [::set-active-panel (merge params {:page :resource-edit})]))

  #_(defroute "/auth#:auth" {:as params}
      (re-frame/dispatch [:events/auth (:auth params)]))

  ;; must be at the end, cause routes matches by order
  (defroute "*" []
    (re-frame/dispatch [::set-active-panel {:page :blank}]))

  ;; --------------------
  (hook-browser-navigation!))

