(ns fhir-face.form
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [clojure.string :as str]
   [fhir-face.style :as style]
   [fhir-face.model :as model]
   [zframes.redirect :refer [href redirect]]
   [fhir-face.widgets :as ws]
   [clojure.set :as set]))

(defonce atom-style (r/atom nil))

(defn style []
  [[:.root {:margin-left "-40px"}]

   [:.input (merge (:thin-gray style/borders)
                   {:outline :none
                    :font-size "16px"
                    :font-weight :bold
                    :margin-left "25px"})]

   [:.non-selectable {:-webkit-user-select :none
                      :-moz-user-select :none
                      :-ms-user-select :none}]
   [:.atts
    [:.active
     ;;[:.name {}]
     [:.type {:color "#777777"}]
     [:.description {:color "#777777"}]]
    [:.unactive {:color "#cccccc"}]
    [:.extra {:color "#cc8888"}]]

   [:.name-value {:display (if (:inline @atom-style) :inline-grid :block)
                  :padding-left "40px"
                  :font-size "20px"
                  ;;:border "1px solid"
                  }]

   [:.item-value {:width :-webkit-fill-available ;;"100%"
                  :font-size "1px"
                  ;;:border "1px solid"
                  :padding "10px 20px 10px 0"
                  ;;:padding-right "20px"
                  ;;:padding-bottom "10px"
                  ;;:padding-top "10px"
                  }]

   [:.material-icons {:font-size "14px"}]

   [:.on-off {:margin-top "15px"
              :margin-bottom "10px"}]

   [:.name {:font-size "20px"
            :margin-left "10px"}]

   [:.type {:font-size "16px"
            :margin-left "10px"}]

   [:.description {:font-size "16px"
                   :margin-left "10px"}]

   [:.coll {;;:padding-left "30px"
            :display :flex
            :flex-wrap :wrap
            :align-items :flex-start
            ;;:justify-content :space-evenly ;;:space-between
            }]

   [:.item  (merge (:thin-gray style/borders)
                   {:display :flex
                    :margin "0 5px 5px 0"
                    :font-size "20px"})]

   [:.error {:font-family :monospace
             :font-size "20px"
             :color :red}]
   ])





(def fhir-primitive-types
  {:string ws/text
   :code ws/text
   :id ws/text
   :markdown ws/text
   :uri ws/text
   :oid ws/text
   :boolean ws/checkbox
   :date ws/date
   :dateTime ws/date-time
   :instant ws/date-time
   :time ws/time-time
   :decimal ws/decimal
   :integer ws/integer
   :unsignedInt ws/unsignedInt
   :positiveInt ws/positiveInt
   ;; :base64Binary

   ;; additional?
   ;; :Resource
   ;; :Extension
   :Reference ws/reference
   :Narrative ws/text
   :number ws/decimal})

#_(defn value-set [s] (str/split s #" \| "))

(defn primitive-component [{:keys [type path enum content]}]
  (let [cmp (if enum ws/select (get fhir-primitive-types type ws/text))]
    [cmp (merge {:path (into (conj model/root-path :data :resource) path)
                 :class :input}
                (cond
                  enum {:items enum}
                  (= type :Reference) {:resourceType (get-in content [:resourceType :enum])}))]))

(def Quantity {:value {:type :decimal}
               :comparator {:type :code :enum ["<" "<=" ">=" ">"]}
               :unit {:type :string}
               :system {:type :uri}
               :code {:type :code}})

(def fhir-basic-types

  {:ContactPoint {:system {:type :code :isRequired true :enum ["phone" "fax" "email" "pager" "url" "sms" "other"]}
                  :value {:type :string}
                  :use {:type :code :enum ["home" "work" "temp" "old" "mobile"]}
                  :rank {:type :positiveInt}
                  :period {:type :Period}}

   :Identifier {:use {:type :code :enum ["usual" "official" "temp" "secondary"]}
                :type {:type :CodeableConcept}
                :system {:type :uri}
                :value {:type :string}
                :period {:type :Period}
                :assigner {:type :Reference
                           :content {:resourceType {:enum ["Organization"]}}
                           ;;:ref-types ["Organization"]
                           }}

   :HumanName {:use {:type :code :enum ["usual" "official" "temp" "nickname" "anonymous" "old" "maiden"]}
               :text {:type :string}
               :family {:type :string}
               :given {:type :string :isCollection true}
               :prefix {:type :string :isCollection true}
               :suffix {:type :string :isCollection true}
               :period {:type :Period}}

   :Coding {:system {:type :uri}
            :version {:type :string}
            :code {:type :code}
            :display {:type :string}
            :userSelected {:type :boolean}}

   :CodeableConcept {:coding {:type :Coding :isCollection true}
                     :text {:type :string}}

   :Period {:start {:type :dateTime}
            :end   {:type :dateTime}}

   :Address {:use  {:type :code :enum ["home" "work" "temp" "old"]}
             :type {:type :code :enum ["postal" "physical" "both"]}
             :text {:type :string}
             :line {:type :string :isCollection true}
             :city {:type :string}
             :district {:type :string}
             :state {:type :string}
             :postalCode {:type :string}
             :country {:type :string}
             :period {:type :Period}}

   :Quantity Quantity
   :Age Quantity
   :Count Quantity
   :Distance Quantity
   :Duration Quantity
   :Money Quantity
   :SimpleQuantity Quantity

   :Range	{:low  {:type :SimpleQuantity}
           :high {:type :SimpleQuantity}}

   :Ratio	{:numerator   {:type :Quantity}
           :denominator	{:type :Quantity}}

   :SampledData	{:origin  {:type :SimpleQuantity}
                 :period {:type :decimal}
                 :factor {:type :decimal}
                 :lowerLimit {:type :decimal}
                 :upperLimit {:type :decimal}
                 :dimensions {:type :positiveInt}
                 :data {:type :string}}

   :Timing {:event {:type :dateTime :isCollection true}
            :repeat {:type :Timing-repeat}
            :code {:type :CodeableConcept}}

   :Timing-repeat {:bounds {:type [:Duration :Range :Period]}
                   :count {:type :integer}
                   :countMax {:type :integer}
                   :duration {:type :decimal}
                   :durationMax {:type :decimal}
                   :durationUnit {:type :code}
                   :frequency {:type :integer}
                   :frequencyMax {:type :integer}
                   :period {:type :decimal}
                   :periodMax {:type :decimal}
                   :periodUnit {:type :code :enum ["s" "min" "h" "d" "wk" "mo" "a"]}
                   :dayOfWeek {:type :code :isCollection true :enum ["mon" "tue" "wed" "thu" "fri" "sat" "sun"]}
                   :timeOfDay {:type :time :isCollection true}
                   :when {:type :code :isCollection true}
                   :offset {:type :unsignedInt}}

   :Signature {:type {:type :Coding :isCollection true}
               :when {:type :instant}
               :who {:type [:Reference :uri]
                     :content {:resourceType
                               {:enum ["Practitioner" "RelatedPerson" "Patient" "Device" "Organization"]}}}
               :onBehalfOf {:type [:Reference :uri]
                            :content {:resourceType
                                      {:enum ["Practitioner" "RelatedPerson" "Patient" "Device" "Organization"]}}}
               :contentType {:type :code}
               :blob {:type :base64Binary}}

   :Annotation {:author {:type [:Reference :string]
                         :content {:resourceType {:enum ["Practitioner" "Patient" "RelatedPerson"]}}}
                :time {:type :dateTime}
                :text {:type :string}}

   :Attachment {:contentType {:type :code}
                :language {:type :code}
                :data {:type :base64Binary}
                :url {:type :uri}
                :size {:type :unsignedInt}
                :hash {:type :base64Binary}
                :title {:type :string}
                :creation {:type :dateTime}}

   :Meta {:versionId {:type :id}
          :lastUpdated {:type :instant}
          :profile {:type :uri :isCollection true}
          :security {:type :Coding :isCollection true}
          :tag {:type :Coding :isCollection true}}
   })

;; ;; Open
;; boolean
;; integer
;; decimal
;; base64Binary
;; instant
;; string
;; uri
;; date
;; dateTime
;; time
;; code
;; oid
;; id
;; unsignedInt
;; positiveInt
;; markdown

;; Annotation
;; Attachment
;; Identifier
;; CodeableConcept
;; Coding
;; Quantity
;; Range
;; Period
;; Ratio
;; SampledData
;; Signature
;; HumanName
;; Address
;; ContactPoint
;; Timing
;; Reference - a reference to another resource
;; Meta


(defn click-dispatch [args]
  {:on-click (fn [e]
               (.stopPropagation e)
               (rf/dispatch args))
   :style {:cursor :pointer}})

(declare zofo)

(defn coll-zofo [r e path attrs]
  (let [items (vec (map-indexed
                    (fn [i _] (let [path* (conj path i)]
                                [:div.item {:key i}
                                 [zofo r e path* (dissoc attrs :isCollection)]
                                 [:span
                                  (update (click-dispatch [::model/expand-collapse-node-deep path*])
                                          :style merge
                                          {:display :flex
                                           :flex-direction :column
                                           :background-color "#f5f5f5"})
                                  [:i.material-icons
                                   (click-dispatch [::model/delete-collection-item path i])
                                   :close]]]))
                    (get-in r path)))]
    (into [:div.coll] items)))

(defn key-name [x] (if (keyword? x) (name x) (str x)))

(defn keys* [x] (if (map? x) (keys x) []))

(defn exists-in? [m ks]
  (or (empty? ks) ;; true ;; (boolean m)
      (let [holder-path (vec (butlast ks))
            holder-value (if (empty? holder-path) m (get-in m holder-path))]
        (contains? holder-value (last ks)))))


(defn zofo [r e path attrs*]
  (let [val? (exists-in? r path)
        val (get-in r path)
        name (last path)
        type (:type attrs*)
        attrs (if-let [cnt (fhir-basic-types type)] (assoc attrs* :content cnt) attrs*)
        collection? (or (:isCollection attrs) (vector? val))
        expanded? (or (empty? path) (get-in e path))
        count-all (count (set/union (-> attrs :content keys* set) (-> val keys* set)))
        count-val (if (coll? val) (count val) 0)
        collapsed-info ;;[:span.non-selectable
        [:span.label.non-selectable
         (click-dispatch [::model/expand-collapse-node path])
         (if collection?
           (str "[" count-val " item" (if (not= 1 count-val) "s") "]")
           (str "{" (if (= count-val count-all) count-all (str count-val "/" count-all))
                " key" (if (not= 1 count-all) "s") "}"))]
        #_[:i.material-icons
           (update (click-dispatch [::model/expand-collapse-node path])
                   :style assoc :margin-left "5px")
           (if (get-in e path) :location_on :play_arrow)]
        ;; ]
        ]
    [:div
     {:class (cond (keyword? name) :name-value
                   (empty? path) :root
                   :else :item-value)}
     (if (keyword? name) [:span.non-selectable
                          (cond-> {:class (cond (:extra? attrs) :extra
                                                (not val?) :unactive
                                                :else :active)}
                            (and val?
                                 (or (and (> count-all 0) (not (fhir-primitive-types type)))
                                     collection?))
                            (merge (click-dispatch [::model/expand-collapse-node-deep path])))

                          [:i.material-icons.on-off
                           (click-dispatch [::model/attribute-on-off path])
                           (if val? "lens" "radio_button_unchecked")]

                          [:span.name (key-name name)]
                          (if type [:span.type (key-name type)])
                          (if (:description attrs) [:span.description (:description attrs)])

                          (if (and collection? expanded?)
                            [:span.label
                             [:i.material-icons
                              (click-dispatch [::model/add-collection-item path])
                              "add_circle_outline"]])])
     (cond
       (not val?) nil

       collection? (if expanded? [coll-zofo r e path attrs] collapsed-info)

       (or (fhir-primitive-types type) (= 0 count-all)) [primitive-component (assoc attrs :path path)]

       (not expanded?) collapsed-info

       :else (let [wids-attrs (let [content (:content attrs)]
                                (reduce (fn [a name] (conj a [zofo r e (conj path name) (get content name)]))
                                        [] (sort (keys content))))

                   ;; add extra attributes - existing in object by path, but not existing in content attrs
                   set-keys-attrs (-> attrs :content keys* set)
                   extra-attrs (reduce (fn [a k]
                                         (if (set-keys-attrs k)
                                           a
                                           (assoc a k {:extra? true :isCollection (vector? (get val k))})))
                                       {} (keys* val))

                   wids-all (let [content extra-attrs]
                              (reduce (fn [a name] (conj a [zofo r e (conj path name) (get content name)]))
                                      wids-attrs (sort (keys content))))]
               (into [:div.atts] wids-all))
       )]))


(defn resource-form [{:keys [type id] :as params}]
  ;;(prn "resource-form" params)
  (let [{:keys [resource resource-expands resource-structure is-fetching error]} @(rf/subscribe [::model/data])]
    [:div.page
     [style/with-common-style (style)]
     [:span
      {:style {:display :flex
               :align-items :baseline}}
      [:h1.h (str (if id "Edit " "New ") (:type params))] [:span.label (:id params)]]

     [:div.bar
      [:span {:style {:display :flex}}
       [:button.btn {:on-click (fn [] (rf/dispatch [::model/expand-all]))} "Expand all"]
       [:button.btn {:on-click (fn [] (rf/dispatch [::model/collapse-all]))} "Collapse all"]
       [:button.btn {:on-click (fn [] (swap! atom-style update :inline not))}
        (str (if (:inline @atom-style) "Classic" "Free") " style")]]
      [:span.action {:on-click #(redirect (href "resource" {:type type}))} (str type " grid")]]

     (if is-fetching
       [:div.loader "Loading"]
       [:div
        [zofo resource resource-expands [] {:content resource-structure}]
        [:div.footer-actions
         [:button.btn {:on-click (fn [] (rf/dispatch [::model/save-resource params]))} "Save"]
         #_[:a.btn.btn-danger {:href (href "locations")} "Cancel"]]
        (if error [:pre.error (with-out-str (cljs.pprint/pprint error))])])
     ]))

(defn for-routes [params]
  ;; (prn "form-panel --------------------------------------" params)
  (rf/dispatch [::model/load-all params])
  [resource-form (:query-params params)])

(def routes {:resource-edit for-routes
             :resource-new for-routes})

