(ns fhir-face.nav
  (:require [reagent.core :as r]
            ;;[re-frame.core :as rf]
            ;;[clojure.string :as str]
            [fhir-face.style :as style]
            [zframes.redirect :refer [href redirect]]
            ;;[graph-view.widgets :as ws]
            ;;[fhir-face.model :as model]
            ))

(def style
  [:.nav-bar {:width "65px"
              ;;:justify-content :space-between
              :align-items :center
              :padding "20px 0"
              :flex-direction :column
              :position :fixed
              :min-height "100vh"
              :background-color "#f4f5f7"
              :display :flex}
   [:.nav-item {;;:display :block
                  :margin-bottom "15px"
                  ;;:color "#333"
                  }
    [:i {:width "40px"
         :height "40px"
         :margin "0 auto"
         :border :none
         :text-align :center
         :box-shadow "0px 0px 2px #aaa"
         :font-size "25px"
         :border-radius "50%"
         :display :table-cell
         :vertical-align :middle
         :cursor :pointer}]]

   ])

(defn nav-bar []
  [:div.nav-bar
   [style/style style]
   ;;"menu"
   ;;"list"
   [:div.nav-item [:i.material-icons
                   {:on-click #(redirect (href "resource"))}
                   "reorder"]]
   [:div.nav-item [:i.material-icons
                   {:on-click #(redirect (href "graph-view"))}
                   "timeline"]]
   ])