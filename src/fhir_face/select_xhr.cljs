(ns fhir-face.select-xhr
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [garden.units :as u]
            [garden.core :as garden]
            [clojure.string :as str]
            [zframes.fetch :as fetch]))

(def style
  [:.select-xhr {:display :inline-flex
                 :align-items :baseline
                 :font-family :system-ui}

   [:.value-xhr {:display :inline-block
                 :position :relative
                 :min-width "200px"
                 :font-size "16px"
                 :font-weight :bold
                 :margin-left "20px"}

    [:.choosen {:display :flex
                :align-items :center
                :cursor :pointer
                :border "1px solid"
                :border-color "#ddd"
                :padding "2px 0"}
     [:.triangle {:margin "0px 10px"}]
     [:.value {:flex-grow "1"}]
     [:.icon {:padding "0px 5px 0 10px"}]]

    [:.drop-menu {:z-index 1
                  :position :absolute
                  :min-width "100%"
                  :width :max-content
                  :background-color "#ffffff"}
     [:.query-loader {:display :flex
                      :align-items :center
                      :width :-webkit-fill-available
                      :border "1px solid"
                      :border-color "#ddd"}
      [:.query {:flex-grow 1
                :padding "2px 2px 2px 16px"
                ;;:width "100%"
                :outline :inherit ;;none
                :font-size :inherit
                :font-weight :inherit
                :border :none}]
      [:.loader {;;:display :inline-block
                 :margin "0px 5px 0px 5px"
                 :font-size "4px"
                 :width "1.5em"
                 :height "1.5em"}]]

    [:.suggestions {:overflow-y :auto
                    :border "1px solid"
                    :border-color "#ddd"
                     :display :flex
                     :flex-direction :column
                     :max-height "300px"}
      [:.info {:text-align :center
               :padding "10px"
               :color :gray}]
      [:.suggestion {:cursor :pointer
                     :padding "0 16px"
                     :line-height "32px"}
       [:&:hover {:background-color "#f1f1f1"}]]]

     ]]])


(rf/reg-sub
 ::common-fetch-params
 (fn [db] {:base-url (get-in db [:config :base-url])
           :id_token (get-in db [:auth :id_token])
           ;; FIXME hack to get all possible resourceType
           :all-resourceTypes (mapv :id (get-in db [:main :data :entity]))}))

;; in our case inner search would be faster than outer
(defn is-child? [par child]
  (if (.isEqualNode par child)
    true
    (some identity
          (map #(is-child? % child)
               (array-seq (.-childNodes par))))))

(defn select-xhr [{:keys [value resourceType] :as opts}]
  (let [common-fetch-params (rf/subscribe [::common-fetch-params]) 

        ;; FIXME show all types is incoming list is empty
        resourceType (if (empty? resourceType) (:all-resourceTypes @common-fetch-params) resourceType)

        value-type (:resourceType value)
        state (r/atom {:resourceType (or value-type (first resourceType))
                       :suggestions []})
        close #(swap! state assoc
                      :active false
                      :suggestions [])
        resourceType (if (and (not (str/blank? value-type)) (not (contains? (set resourceType) value-type)))
                       (conj resourceType value-type)
                       resourceType)
        doc-click-listener (fn [e]
                             (let [outer-click? (not (is-child? (:root-node @state) (.-target e)))]
                               (when (and outer-click? (:active @state))
                                 (when-let [f (:on-blur opts)] (f e))
                                 (swap! state assoc :active false))))]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (let [root (r/dom-node this)]
          (swap! state assoc :root-node root))
        (.addEventListener js/document "click" doc-click-listener))

      :component-will-unmount
      (fn [this]
        (.removeEventListener js/document "click" doc-click-listener))

      :reagent-render
      (fn [{:keys [value on-change label-fn value-fn placeholder] :as props}]
        (let [label-fn (or label-fn pr-str)
              value-fn (or value-fn identity)
              fetch-load (fn [text]
                           ;;(prn @common-fetch-params)
                           (swap! state assoc :loading true)
                           (fetch/json-fetch
                            {:uri (str (:base-url @common-fetch-params) "/" (:resourceType @state))
                             :token (:id_token @common-fetch-params)
                             :params (cond-> {:_count 50} ;; :_sort "name"}
                                       (and text (not (str/blank? text))) (assoc :_text text))
                             :success (fn [x]
                                        (swap! state assoc
                                               :loading false
                                               :suggestions (mapv (comp value-fn :resource) (:entry x))))}))]
          [:div.select-xhr
           [:style (garden/css style)]

           (into [:select {:class :input
                           :value (str (:resourceType @state))
                           :on-change #(let [v (.. % -target -value)]
                                         (swap! state assoc :resourceType v :suggestions [])
                                         (when on-change (on-change nil))
                                         (close))}]
                 (mapv (fn [x] [:option x]) resourceType))

           [:div.value-xhr
            [:div.choosen
             {:on-click (fn [_] (if (:active @state)
                                  (close)
                                  (do
                                    (fetch-load nil)
                                    (swap! state assoc :active true))))}
             [:span.triangle "▾"]
             [:span.value (if value (label-fn value) placeholder)]
             (when value
               [:span.icon
                {:on-click (fn [e]
                             (when on-change (on-change nil))
                             (.stopPropagation e))}
                [:i.material-icons :close]])]

            (when (:active @state)
              [:div.drop-menu
               [:div.query-loader
                [:input.query
                 {:type "text"
                  :placeholder "Search on enter..."
                  :auto-focus true
                  :on-key-down (fn [ev] (when (= 13 (.-keyCode ev)) (fetch-load (.. ev -target -value))))}]
                (when (:loading @state) [:div.loader "loading..."])]
               (when-not (:loading @state)
                 [:div.suggestions
                  (cond
                    (empty? (:suggestions @state)) [:div.info "No results"]
                    :else (for [i (:suggestions @state)] ^{:key (pr-str i)}
                            ;; (:suggestions @state) already mapped by value-fn on upload fetch results
                            [:div.suggestion
                             {:on-click #(do (when on-change (on-change i))
                                             (close))}
                             (label-fn i)]))])])
            ]]))})))

