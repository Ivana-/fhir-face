(ns fhir-face.widgets
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as str]
            [fhir-face.select-xhr :as xhr]))

(rf/reg-sub
 ::get-value
 (fn [db [_ path]] (get-in db path)))

(rf/reg-event-db
 ::set-value
 (fn [db [_ path v]] (assoc-in db path v)))

#_(defn text-db [{:keys [path min-size max-size] :as params}]
  (let [value (str @(rf/subscribe [::get-value path]))
        min-size* (or min-size 3)
        max-size* (or max-size 100)]
    [:input (-> params
                (dissoc :path :min-size :max-size)
                (assoc
                 :type "text"
                 :value value
                 :size (max min-size* (min max-size* (int (* 1.5 (+ 1 (.-length value))))))
                 :on-change #(rf/dispatch [::set-value path (.. % -target -value)])))]))

(defn select [{:keys [path items] :as params}]
  (let [value (str @(rf/subscribe [::get-value path]))]
    (into
     [:select (-> params
                  (dissoc :path :items)
                  (assoc
                   :value value
                   :on-change #(rf/dispatch [::set-value path (.. % -target -value)])))
      [:option ""]
      (if (and (not (str/blank? value)) (not (contains? (set items) value))) [:option value])]
     (mapv (fn [x] [:option x]) items))))


(defn- node-auto-width [node]
  (set! (.. node -style -width) "1px")
  (set! (.. node -style -width) (str (+ (.. node -scrollWidth) 0) "px")))

(defn- node-auto-height [node]
  (set! (.. node -style -height) "1px")
  (set! (.. node -style -height) (str (+ (.. node -scrollHeight) 2) "px")))

(defn- node-auto-size [node]
  (if node (case (.. node -tagName)
             "TEXTAREA" (node-auto-height node)
             "INPUT" (node-auto-width node))))

(defn text [{:keys [path] :as params}]
  (let [value (str @(rf/subscribe [::get-value path]))
        multi-line? (r/atom (or (> (.-length value) 50) (str/includes? value "\n")))]
    (r/create-class
     {:reagent-render (fn [params]
                        (let [value (str @(rf/subscribe [::get-value path]))
                              props (-> params
                                        (dissoc :path)
                                        (assoc :value value))]
                          (conj
                           (if @multi-line?
                             [:div
                              {:style {:display :flex ;;:inline-flex ;;:flex
                                       ;;:width "100%"
                                       }}
                              [:textarea (-> props
                                             (assoc :on-change (fn [e]
                                                                 (node-auto-height (.. e -target))
                                                                 (rf/dispatch [::set-value path (.. e -target -value)]))
                                                    :style {:-webkit-box-sizing :border-box
                                                            :-moz-box-sizing :border-box
                                                            :box-sizing :border-box
                                                            :width "100%"
                                                            :resize :none
                                                            :overflow :hidden
                                                            ;;:margin "0 0 2px 0"
                                                            }))]]
                             [:span
                              {:style {:display :inline-flex
                                       ;;:vertical-align :bottom
                                       :align-items :center
                                       }}
                              [:input (-> props
                                          (assoc :type "text"
                                                 :on-change (fn [e]
                                                              (node-auto-width (.. e -target))
                                                              (rf/dispatch [::set-value path (.. e -target -value)]))
                                                 :style {:min-width "20px"
                                                         :padding "2px"}))]])
                           [:i.material-icons
                            {:on-click (fn [e] (.stopPropagation e) (swap! multi-line? not))
                             ;; :style {:margin-left "5px" :margin-top "5px"}
                             }
                            (if @multi-line? :location_on :play_arrow)])))

      :component-did-mount (fn [this]
                             ;;(prn "component-did-mount")
                             (node-auto-size (.. (r/dom-node this) -firstChild)))
      :component-did-update (fn [this]
                              ;;(prn "component-did-update")
                              (let [node (.. (r/dom-node this) -firstChild)]
                                (node-auto-size node)
                                (.focus node)))})))


(defn checkbox [{:keys [path] :as params}]
  [:input (-> params
              (dissoc :path)
              (assoc
               :type "checkbox"
               :checked (= "true" (str @(rf/subscribe [::get-value path])))
               :on-change #(rf/dispatch [::set-value path (.. % -target -checked)])))])

(defn date [{:keys [path] :as params}]
  [:input (-> params
              (dissoc :path)
              (assoc
               :type "date"
               :value (str @(rf/subscribe [::get-value path]))
               :on-change #(rf/dispatch [::set-value path (.. % -target -value)])))])

(defn date-time [{:keys [path] :as params}]
  (let [v (str @(rf/subscribe [::get-value path]))
        cnt-v (count v)
        value (cond
                (= 0 cnt-v) ""
                (>= cnt-v 16) (subs v 0 16)
                :else (str v (subs "2018-01-01T00:00" cnt-v)))]
    [:input (-> params
                (dissoc :path)
                (assoc
                 :type "datetime-local"
                 :value value
                 :on-change #(rf/dispatch [::set-value path (str (.. % -target -value) ":00Z")])))]))

(defn time-time [{:keys [path] :as params}]
  [:input (-> params
              (dissoc :path)
              (assoc
               :type "time"
               :value (str @(rf/subscribe [::get-value path]))
               :on-change #(rf/dispatch [::set-value path (.. % -target -value)])))])

;; js/isNaN

(defn decimal [{:keys [path] :as params}]
  [:input (-> params
              (dissoc :path)
              (assoc
               :type "number"
               :step "0.000000000000001"
               :value (str @(rf/subscribe [::get-value path]))
               :on-change #(rf/dispatch [::set-value path (js/parseFloat (.. % -target -value))])))])


(defn integer [{:keys [path] :as params}]
  [:input (-> params
              (dissoc :path)
              (assoc
               :type "number"
               ;;:step "1"
               :value (str @(rf/subscribe [::get-value path]))
               :on-change #(rf/dispatch [::set-value path (js/parseFloat (.. % -target -value))])))])

(defn unsignedInt [{:keys [path] :as params}]
  [:input (-> params
              (dissoc :path)
              (assoc
               :type "number"
               :min "0"
               ;;:step "1"
               :value (str @(rf/subscribe [::get-value path]))
               :on-change #(rf/dispatch [::set-value path (js/parseFloat (.. % -target -value))])))])

(defn positiveInt [{:keys [path] :as params}]
  [:input (-> params
              (dissoc :path)
              (assoc
               :type "number"
               :min "1"
               ;;:step "1"
               :value (str @(rf/subscribe [::get-value path]))
               :on-change #(rf/dispatch [::set-value path (js/parseFloat (.. % -target -value))])))])



(defn get-display-from [r k]
  (let [v (get r k)]
    (cond
      (str/blank? v) nil
      (and (= :name k)
           (or (vector? v) (:given v))) (let [n (if (vector? v) (first v) v)]
                                          (->> [(:text n) (str/join " " (conj (or (:given n) []) (:family n)))]
                                               (remove str/blank?)
                                               first))
      (string? v) v)))

(defn resource-display [r]
  (if (nil? r)
    nil
    (or (->> [:display :name]
             (map #(get-display-from r %))
             (remove str/blank?)
             first)
        (->> [:patient
              :subject ;; Encounter
              :beneficiary ;;:subscriber :policyHolder ;; Coverage
              :details ;; AidboxNotification
              ]
             (map #(resource-display (% r)))
             (remove str/blank?)
             first))))


(defn reference [{:keys [path resourceType settings] :as params}]
  (let [value @(rf/subscribe [::get-value path])]
    [xhr/select-xhr (merge
                   {:value value
                   ;;:placeholder "Xhr select"
                   ;;:on-blur identity
                   :on-change #(rf/dispatch [::set-value path %])
                   :resourceType resourceType}
                   (case (:fhir-server-type settings)
                     :hapi
                     {:label-fn (fn [x] (let [[rt id] (str/split (:reference x) #"/")] (str (if id (str "[" id "]  ")) (:display x))))
                      :value-fn (fn [x] (let [d (resource-display x)]
                                  (cond-> {:reference (str (:resourceType x) "/" (:id x))}
                                    (not (str/blank? d)) (assoc :display d))))
                      :value-type (str (first (str/split (:reference value) #"/")))}
                     {:label-fn #(str "[" (:id %) "]  " (:display %))
                      :value-fn #(let [d (resource-display %)]
                                  (cond-> (select-keys % [:id :resourceType])
                                    (not (str/blank? d)) (assoc :display d)))
                      :value-type (str (:resourceType value))}))]))
