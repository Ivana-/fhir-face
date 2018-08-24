(ns fhir-face.grid
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [fhir-face.style :as style]
            [fhir-face.model :as model]
            [zframes.redirect :refer [href redirect]]
            [fhir-face.widgets :as ws]))

(def style
  [[:.input (merge (:thin-gray style/borders)
                   {:outline :none
                    :font-size "16px"
                    :font-weight :bold})]

   [:.item {;;:font-size "14px"
            :display :flex
            :justify-content :space-between
            :cursor :pointer
            :font-family :system-ui
            :padding "5px 0 5px 0"
            :border-bottom "1px solid"
            :border-color "#cccccc"}
    [:&:hover {:background-color "#f1f1f1"}]
    [:.column {:font-size "20px"
               ;;:font-weight :bold
               :margin-left "10px"}]]
   ])


(defn search-box [params]
  (let [on-key-down (fn [ev]
                      (when (= 13 (.-which ev))
                        (let [v (.. ev -target -value)]
                          (redirect (href "resource"
                                          (if (str/blank? v)
                                            (dissoc params :_text)
                                            (merge params {:_text v})))))))]
    [:div.search {:style {:display :flex}}
     [:i.material-icons :search]
     [:input (cond-> {:class :input
                      :style {:width "100%"}
                      ;;:type :search
                      :on-key-down on-key-down
                      ;;:auto-focus true
                      :default-value (:_text params)
                      :placeholder "Search on enter...."}
     ;;           ;;;; (str/blank? (:_text params)) (assoc :value "")
               )]]))


(defn resource-grid [params]
  ;; (prn "resour;; ce-grid" params)
  (let [data @(rf/subscribe [::model/data])
        entity (mapv :id (:entity data))
        items (:resource-grid data)
        {:keys [type]} params]
    [:div.page
     [style/with-common-style style]

     [:span
      {:style {:display :flex
               :align-items :baseline}}
      [:h1.h (if type (str type " grid") "Select resource type")]]

     [:div.bar
      [:span {:style {:display :flex}}
       (into [:select {:class :input
                       :value (str type)
                       :on-change #(let [v (.. % -target -value)]
                                     (redirect (href "resource"
                                                     (if (str/blank? v)
                                                       {} ;;(dissoc params :type)
                                                       (merge params {:type v})))))}
              [:option ""]]
             (mapv (fn [x] [:option x]) entity))
       (if type [search-box params])]
      (if type [:span.action {:on-click #(redirect (href "resource" "new" {:type type}))} (str "New " type)])]

     (cond
       (:is-fetching data) [:div.loader "Loading"]

       (and type (= type (get-in data [:query-params :type])))
       (if (empty? items)
         [:div "Nothing to show"]
         (let [flex (fn [x] {:style {:flex-basis (str x "%")}})]
           [:div (for [i items]
                   (let [d (ws/resource-display i)]
                     [:div.item {:key (:id i)
                                 :on-click #(redirect (href "resource" "edit" {:type type :id (:id i)}))}
                      [:span.column (flex (if d 20 90)) (:id i)]
                      (if d [:span.column (flex 70) d])
                      [:span.column #_(flex 10) (.-length (str i))]
                      ]))]))
       )]))

(def routes {:resource-grid (fn [params]
                              ;; (prn "grid-panel --------------------------------------" params)
                              (rf/dispatch [::model/load-all params])
                              [resource-grid (:query-params params)])})

