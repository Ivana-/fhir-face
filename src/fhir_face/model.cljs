(ns fhir-face.model
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [zframes.redirect :refer [href redirect]]
            [zframes.fetch :refer [fetch-promise error-data error-message]]))

(def root-path [:main])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Loader
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


#_(comment
(defn dec-counter-check-finish [db]
  (let [counter-path (conj root-path :tmp :fetching-counter)
        fetching-counter (dec (or (get-in db counter-path) 0))]
    (cond-> (assoc-in db counter-path fetching-counter)
      (<= fetching-counter 0)
      (update-in root-path (fn [x] (-> x
                                       (update :data (fn [d] (merge
                                                              (dissoc d :is-fetching)
                                                              (-> (:tmp x)
                                                                  (dissoc :fetching-counter)
                                                                  ;;(update :query-params #(merge % (:query-params d)))
                                                                  ))))
                                       (dissoc :tmp)))))))
(rf/reg-event-fx
 ::load-all
 (fn [{db :db} [_ {page :page params :query-params}]]
   ;; (prn "::load-all" params)
   (let [{:keys [type _text id]} params
         base-url (get-in db [:config :base-url])
         token (get-in db [:auth :id_token])
         data (get-in db (conj root-path :data))
         fetch-vec (cond-> []
                     (nil? (:entity data))
                     (conj {:uri (str base-url "/Entity")
                            :params {:_count 500 :.type "resource" :_sort ".id"}
                            :token token
                            :success {:event ::entity-loaded}})

                     (and type (not= type (get-in db (conj root-path :data :query-params :type))))
                     ;; http://localhost:8080/Attribute?_format=yaml&entity=Patient
                     (conj {:uri (str base-url "/Attribute")
                            :params {:entity type}
                            :token token
                            :success {:event ::resource-structure-loaded
                                      :params params}})

                     (and type (= :resource-grid page))
                     (conj {:uri (str base-url "/" type)
                            :token token
                            :params (cond-> {:_count 50} ;; :_sort "name"}
                                      (and _text (not (str/blank? _text))) (assoc :_text _text))
                            :success {:event ::resource-grid-loaded}})

                     (and type id (= :resource-edit page))
                     (conj {:uri (str base-url "/" type "/" id)
                            :token token
                            :success {:event ::resource-item-loaded}})
                     )
         fetching-counter (count fetch-vec)
         new? (= :resource-new page)
         new-resource {:resourceType type}]
     (if (> fetching-counter 0)
       {:json/fetch fetch-vec
        :db (cond-> (-> db
                        (assoc-in (conj root-path :tmp) {:fetching-counter fetching-counter
                                                         :query-params params})
                        (assoc-in (conj root-path :data :is-fetching) true)
                        (update-in (conj root-path :data) dissoc :error))
              new? (assoc-in (conj root-path :tmp :resource) new-resource))}
       {:db (cond-> (-> db
                        (update-in (conj root-path :data) dissoc :is-fetching)
                        (update-in (conj root-path :data) dissoc :error))
              new? (assoc-in (conj root-path :data :resource) new-resource))}))))

(rf/reg-event-db
 ::entity-loaded
 (fn [db [_ {:keys [data]}]]
   ;;(prn "--------------- entity loaded")
   (-> db
       (assoc-in (conj root-path :tmp :entity) (mapv :resource (:entry data)))
       dec-counter-check-finish)))

(rf/reg-event-db
 ::resource-structure-loaded
 (fn [db [_ {:keys [data params]}]]
   ;;(prn "--------------- structure loaded" params)
   (let [type (:type params)
         s (reduce (fn [a x]
                     (update-in a
                                (->> (:path x) (interpose :content) (mapv keyword))
                                #(merge % (cond-> x
                                              (:type x) (assoc ;;:type-full (:type x)
                                                               :type (keyword (get-in x [:type :id])))
                                              true (dissoc :resource :path :id :meta :resourceType)))))
                   {} (mapv :resource (:entry data)))
         s-with-meta (merge s {:meta {:type :Meta}
                               :resourceType {:type :code}})]
     (-> db
         (assoc-in (conj root-path :tmp :resource-structure) s-with-meta)
         ;;(assoc-in (conj root-path :tmp :type) type)
         dec-counter-check-finish))))

(rf/reg-event-db
 ::resource-grid-loaded
 (fn [db [_ {:keys [data]}]]
   ;;(prn "--------------- grid loaded")
   (-> db
       (assoc-in (conj root-path :tmp :resource-grid) (mapv :resource (:entry data)))
       dec-counter-check-finish)))

(rf/reg-event-db
 ::resource-item-loaded
 (fn [db [_ {data :data}]]
   ;;(prn "--------------- item loaded")
   (-> db
       (assoc-in (conj root-path :tmp :resource) data)
       dec-counter-check-finish)))
)

;; Via promises

(rf/reg-event-fx
 ::load-all
 (fn [{db :db} [_ {page :page params :query-params}]]
   ;;(prn "test-fetch" page params)
   (let [{:keys [type _text id]} params
         base-url (get-in db [:config :base-url])
         token (get-in db [:auth :id_token])
         data (get-in db (conj root-path :data))
         fp (fn [k] (conj root-path :data k))
         fetching-path (fp :is-fetching)
         get-res-structure (fn [resp]
                             (let [s (reduce (fn [a x]
                                               (update-in a
                                                          (->> (:path x) (interpose :content) (mapv keyword))
                                                          #(merge % (cond-> x
                                                                      (:type x) (assoc ;;:type-full (:type x)
                                                                                 :type (keyword (get-in x [:type :id])))
                                                                      true (dissoc :resource :path :id :meta :resourceType)))))
                                             {} (mapv :resource (get-in resp [:data :entry])))
                                   s-with-meta (merge s {:meta {:type :Meta}
                                                         :resourceType {:type :code}})]
                               s-with-meta))]

     ;; good series queries

     #_(-> (if (:entity data) (js/Promise.resolve nil) (fetch-promise {:uri (str base-url "/Entity")
                                                                     :params {:_count 500 :.type "resource" :_sort ".id"}
                                                                     :token token}))

         (.then (fn [x] (js/Promise.all [(cond-> {}
                                           x (assoc :entity (mapv :resource (get-in x [:data :entry]))))
                                         (if (and type (not= type (get-in data [:query-params :type])))
                                           (fetch-promise {:uri (str base-url "/Attribute")
                                                           :params {:entity type}
                                                           :token token}))])))
         (.then (fn [[x y]] (js/Promise.all [(cond-> x
                                               y (assoc :resource-structure (get-res-structure y)))
                                             (if (and type (= :resource-grid page))
                                               (fetch-promise {:uri (str base-url "/" type)
                                                               :token token
                                                               :params (cond-> {:_count 50} ;; :_sort "name"}
                                                                         (and _text (not (str/blank? _text)))
                                                                         (assoc :_text _text))}))])))
         (.then (fn [[x y]] (js/Promise.all [(cond-> x
                                               y (assoc :resource-grid (mapv :resource (get-in y [:data :entry]))))
                                             (if (and type id (= :resource-edit page))
                                               (fetch-promise {:uri (str base-url "/" type "/" id)
                                                               :token token}))])))
         (.then (fn [[x y]]
                  (let [r (cond-> x
                            y (assoc :resource (:data y)))]
                    (rf/dispatch [::set-values-by-paths
                                  (reduce (fn [a [k v]] (if v (assoc a (fp k) v) a))
                                          {(fp :error) nil
                                           fetching-path false
                                           (fp :query-params) params} r)]
                                 ))))

         (.catch (fn [e] (rf/dispatch [::set-values-by-paths {(conj root-path :data :error) (str e)
                                                              fetching-path false}]))))

     ;; parallel query

     (-> (js/Promise.all [(if-not (:entity data)
                            (fetch-promise {:uri (str base-url "/Entity")
                                            :params {:_count 1000 :.type "resource" :_sort ".id"}
                                            :token token}))
                          (if (and type (not= type (get-in data [:query-params :type])))
                            (fetch-promise {:uri (str base-url "/Attribute")
                                            :params {:entity type}
                                            :token token}))
                          (if (and type (= :resource-grid page))
                            (fetch-promise {:uri (str base-url "/" type)
                                            :token token
                                            :params (cond-> {:_count 50} ;; :_sort "name"}
                                                      (and _text (not (str/blank? _text))) (assoc :_text _text))}))
                          (if (and type id (= :resource-edit page))
                            (fetch-promise {:uri (str base-url "/" type "/" id)
                                            :token token}))
                          (if (and (= :graph-view page) (empty? (:references-graph data)))
                            (fetch-promise {:uri (str base-url "/Attribute")
                                            :params {:_text "resourceType\"]" ;; id: Immunization.patient.resourceType, path: [patient, resourceType]
                                                     :_count 2000
                                                     :_elements "resource,enum"}
                                            :token token}))])
         (.then (fn [[e a g i rg]]
                  (rf/dispatch [::set-values-by-paths
                                (cond-> {(fp :error) nil
                                         fetching-path false
                                         (fp :query-params) params}
                                  e (assoc (fp :entity) (mapv :resource (get-in e [:data :entry])))
                                  a (assoc (fp :resource-structure) (get-res-structure a))
                                  g (assoc (fp :resource-grid) (mapv :resource (get-in g [:data :entry])))
                                  i (assoc (fp :resource) (:data i))
                                  rg (assoc (fp :references-graph)
                                            (reduce (fn [a x] (update-in a [(get-in x [:resource :resource :id]) :edges]
                                                                         #(into (or % #{}) (set (get-in x [:resource :enum])))))
                                                    {} (get-in rg [:data :entry])))
                                  (= :resource-new page) (assoc (fp :resource) {:resourceType type})
                                  )])))

         (.catch (fn [e] (rf/dispatch [::set-values-by-paths {(fp :error) (str e)
                                                              fetching-path false}]))))

     #_(-> (js/Promise.resolve {:qqq 333})
         (.then #(assoc-in % [:transit :zazaza] "zazaza")))

     {:db (assoc-in db fetching-path true)}
     )))

#_(rf/reg-event-fx
 ::load-attribute
 (fn [{db :db} [_ {page :page params :query-params}]]
   ;;(prn "test-fetch" page params)
   (let [;;{:keys [type _text id]} params
         base-url (get-in db [:config :base-url])
         token (get-in db [:auth :id_token])
         data (get-in db (conj root-path :data))
         fp (fn [k] (conj root-path :data k))
         fetching-path (fp :is-fetching)
         ]


     ;;enum: [DiagnosticReport, ImagingStudy, Immunization, MedicationAdministration,
     ;;       MedicationDispense, Observation, Procedure, SupplyDelivery]
     ;;resource: {id: ChargeItem, resourceType: Entity}

     (-> (fetch-promise {:uri (str base-url "/Attribute")
                         :params {:_text "resourceType\"]"
                                   :_count 2000
                                   :_elements "resource,enum"}
                         ;;id: Immunization.patient.resourceType
                         ;;enum: [Patient]
                         ;;path: [patient, resourceType]
                         :token token})
         (.then (fn [x] (rf/dispatch [::set-values-by-paths
                                      {(fp :error) nil
                                         fetching-path false
                                         ;;(fp :query-params) params
                                         (fp :resource-graph) ;;(mapv :resource (get-in x [:data :entry]))
                                       (reduce (fn [a x] (update-in a [(get-in x [:resource :resource :id]) :edges]
                                                                    #(into (or % #{}) (set (get-in x [:resource :enum])))))
                                               {} (get-in x [:data :entry]))

                                       }])))

         (.catch (fn [e] (rf/dispatch [::set-values-by-paths {(fp :error) (str e)
                                                              fetching-path false}]))))
     {:db (assoc-in db fetching-path true)})))

(rf/reg-sub
 ::data
 (fn [db] (get-in db (conj root-path :data))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UI actions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(rf/reg-event-db
 ::set-value
 (fn [db [_ path v]] (assoc-in db (into (conj root-path :data :resource) path) v)))

;; (defn add-del-val [db path data-key v]
;;   (let [k (last path)]

;;     (prn path data-key v)

;;     (update-in db (into (conj root-path :data data-key) (butlast path))
;;                #(if (contains? % k) (dissoc % k) (assoc % k v)))))

(def expand-existing-item {})
(def expand-non-existing-item nil)

(defn to-expand-structure [r]
  (cond
    (map? r) (reduce (fn [a [k v]] (assoc a k (to-expand-structure v))) {} r)
    (vector? r) (mapv to-expand-structure r)
    #_(:a (reduce (fn [{a :a i :i} v] {:a (assoc a i (to-expand-structure v)) :i (inc i)}) {:a {} :i 0} r))
    :else expand-existing-item))

(defn get-expanded-structure [db path] (to-expand-structure (get-in db (into (conj root-path :data :resource) path))))

(defn to-expand-structure-1l [r]
  (cond
    ;;(map? r) (reduce (fn [a [k v]] (assoc a k expand-existing-item)) {} r)
    (vector? r) (mapv (fn [_] expand-non-existing-item) r) ;;(mapv (fn [_] expand-existing-item) r)
    :else expand-existing-item))

(defn get-expanded-structure-1l [db path] (to-expand-structure-1l (get-in db (into (conj root-path :data :resource) path))))

;; ;;(defn assoc-in* [m keys v] (let [x (get-in m keys)] (if (or (nil? x) (map? x)) (assoc-in m keys v) m)))

;; (defn collapse-node [db path]
;;   ;;(update-in db (into (conj root-path :data :resource-expands) (butlast path)) #(dissoc % (last path)))
;;   (assoc-in db (into (conj root-path :data :resource-expands) path) false))

;; (defn expand-node [db path]
;;   ;;(update-in db (into (conj root-path :data :resource-expands) (butlast path)) #(assoc  % (last path) expand-existing-item))
;;   (assoc-in db (into (conj root-path :data :resource-expands) path) expand-existing-item))

;; (defn expand-node-deep [db path]
;;   #_(update-in db (into (conj root-path :data :resource-expands) (butlast path))
;;              #(assoc % (last path) (to-expand-structure (get-in db (into (conj root-path :data :resource) path)))))
;;   (assoc-in db (into (conj root-path :data :resource-expands) path)
;;             (to-expand-structure (get-in db (into (conj root-path :data :resource) path)))))

(rf/reg-event-db
 ::delete-collection-item
 (fn [db [_ path i]]
   ;;(prn "::delete-collection-item" path i)
   (let [del-ind (fn [v] (vec (concat (subvec v 0 i) (subvec v (inc i)))))]
     (-> db
         (update-in (into (conj root-path :data :resource) path) del-ind)
         (update-in (into (conj root-path :data :resource-expands) path) del-ind)))))

(rf/reg-event-db
 ::add-collection-item
 (fn [db [_ path]]
   ;;(prn "::add-collection-item" path)
   (-> db
       (update-in (into (conj root-path :data :resource) path) #(conj (or % []) nil))
       (update-in (into (conj root-path :data :resource-expands) path) #(conj (if (vector? %) % []) expand-existing-item)))))


(rf/reg-event-db
 ::attribute-on-off
 (fn [db [_ path]]
   ;;(prn "::attribute-on-off")
   (let [k (last path)
         pre-path (into (conj root-path :data :resource) (butlast path))
         exp-path (into (conj root-path :data :resource-expands) path)]
         #_(update-in (into (conj root-path :data :resource) (butlast path))
                      #(if (contains? % k) (dissoc % k) (assoc % k nil)))
     (if (contains? (get-in db pre-path) k)
       (-> db
           (update-in pre-path #(dissoc % k))
           (assoc-in exp-path expand-non-existing-item))
       (-> db
           (update-in pre-path #(assoc % k nil))
           (assoc-in exp-path (get-expanded-structure db path)))))))


(defn swap-val-nil [db path val]
  (let [full-path (into (conj root-path :data :resource-expands) path)
        v (get-in db full-path)]
    (assoc-in db full-path (if v expand-non-existing-item val))))

(rf/reg-event-db
 ::expand-collapse-node
 (fn [db [_ path]]
   ;;(prn "::expand-collapse-node" path)
   (swap-val-nil db path (get-expanded-structure-1l db path))))

(rf/reg-event-db
 ::expand-collapse-node-deep
 (fn [db [_ path]]
   ;;(prn "::expand-collapse-node-deep" path)
   (swap-val-nil db path (get-expanded-structure db path))))

(rf/reg-event-db
 ::expand-all
 (fn [db _]
   (assoc-in db (conj root-path :data :resource-expands)
             ;;(to-expand-structure (get-in db (conj root-path :data :resource)))
             (get-expanded-structure db []))))

(rf/reg-event-db
 ::collapse-all
 (fn [db _] (update-in db (conj root-path :data) #(dissoc % :resource-expands))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Saver
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#_(comment
(rf/reg-event-fx
 ::save-resource
 (fn [{db :db} [_ {:keys [type id] :as params}]]
   ;;(prn "::save-resource" params type)
   (let [base-url (get-in db [:config :base-url])
         res (get-in db (conj root-path :data :resource))
         ;;id (:id res)
         ]
     {:db (update-in db (conj root-path :data) dissoc :error)
      :json/fetch
      (merge {:body res
              :token (get-in db [:auth :id_token])
              :success {:event ::save-resource-success}
              :error {:event ::save-resource-error}}
             (if id
               {:uri (str base-url "/" type "/" id)
                :method "PUT"}
               {:uri (str base-url "/" type)
                :method "POST"}))})))

(rf/reg-event-fx
 ::save-resource-success
 (fn [{db :db} [_ {data :data}]]
   (redirect (href "resource" {:type (:resourceType data)}))
   {}))

(rf/reg-event-fx
 ::save-resource-error
 (fn [{db :db} [_ {data :data}]]
   {:db (assoc-in db (conj root-path :data :error) data)}))
)


;; Via promises

(rf/reg-event-fx
 ::save-resource
 (fn [{db :db} [_ {:keys [type id] :as params}]]
   ;;(prn "::save-resource" params type)
   (let [base-url (get-in db [:config :base-url])
         res (get-in db (conj root-path :data :resource))
         ;;id (:id res)
         ]
     (-> (fetch-promise (merge {:body res
                                :token (get-in db [:auth :id_token])}
                               (if id
                                 {:uri (str base-url "/" type "/" id)
                                  :method "PUT"}
                                 {:uri (str base-url "/" type)
                                  :method "POST"})))
         (.then (fn [_] (redirect (href "resource" {:type type}))))
         (.catch (fn [e] (rf/dispatch [::set-values-by-paths {(conj root-path :data :error)
                                                              (or
                                                               (dissoc (:data (error-data e)) :resourceType)
                                                               (error-message e))}]))))
     {:db (update-in db (conj root-path :data) dissoc :error)})))


(rf/reg-event-db
 ::set-values-by-paths
 (fn [db [_ paths-values]] (reduce (fn [a [k v]] ((if (vector? k) assoc-in assoc) a k v)) db paths-values)))



#_(comment
  ;; https://gist.github.com/pesterhazy/74dd6dc1246f47eb2b9cd48a1eafe649
(ns my.promises
  "Demo to show different approaches to handling promise chains in ClojureScript
  In particular, this file investigates how to pass data between Promise
  callbacks in a chain.
  See Axel Rauschmayer's post
  http://2ality.com/2017/08/promise-callback-data-flow.html for a problem
  statement.
  The examples is this: based on n, calculate (+ (square n) n), but with each step
  calculated asynchronously. The problem for a Promise-based solution is that the
  sum step needs access to a previous value, n.
  Axel's solution 1 is stateful and not idiomatic in Clojurescript.
  Solution 1 (nested scopes) is implemented in test3.
  Solution 2 (multiple return values) is implemented in test1 and test2.
  For reference, a synchronous implementation is implemented in test0."
  (:refer-clojure :exclude [resolve]))

(enable-console-print!)

;; helpers for working with promises in CLJS

(defn every [& args]
  (js/Promise.all (into-array args)))

(defn soon
  "Simulate an asynchronous result"
  ([v] (soon v identity))
  ([v f] (js/Promise. (fn [resolve]
                        (js/setTimeout #(resolve (f v))
                                       500)))))

(defn resolve [v]
  (js/Promise.resolve v))

;; helpers

(defn square [n] (* n n))

;; test0

(defn test0
  "Synchronous version - for comparison
  The code has three steps:
  - get value for n
  - get square of n
  - get sum of n and n-squared
  Note that step 3 requires access to the original value, n, and to the computed
  value, n-squared."
  []
  (let [n 5
        n-squared (square 5)
        result (+ n n-squared)]
    (prn result)))

;; test1

(defn square-step [n]
  (soon (every n (soon n square))))

(defn sum-step [[n squared-n]] ;; Note: CLJS destructuring works with JS arrays
  (soon (+ n squared-n)))

(defn test1
  "Array approach, flat chain: thread multiple values through promise chain by using Promise.all"
  []
  (-> (resolve 5)
      (.then square-step)
      (.then sum-step)
      (.then prn)))

;; test2

(defn to-map-step [array]
  (zipmap [:n :n-squared] array))

(defn sum2-step [{:keys [n n-squared] :as m}]
  (soon (assoc m :result (+ n n-squared))))

(defn test2
  "Accumulative map approach, flat chain: add values to CLJS map in each `then` step, making
  it possible for later members of the chain to access previous results"
  []
  (-> (resolve 5)
      (.then square-step)
      (.then to-map-step)
      (.then sum2-step)
      ;; Note: `(.then :result)` doesn't work because `:result` is not
      ;; recognized as a function. So we need to wrap it in an anon fn.
      ;; This could be easily fixed by adding a CLJS `then` function that
      ;; has a more inclusive notion of what a function is.
      (.then #(:result %))
      (.then prn)))

;; test3

(defn square-step-fn [n]
  ;; This could be called a "resolver factory" fn. It's a higher-order function
  ;; that returns a resolve function. `n` is captured in a closure.
  (fn [n-squared]
    (soon (+ n n-squared))))

(defn square-and-sum-step [n]
  (-> (soon (square n))
      ;; note that square-step-fn is _called_ here, not referenced, in order to
      ;; provide its inner body with access to the previous result, `n`.
      (.then (square-step-fn n))))

(defn test3
  "Nested chain approach: instead of a flat list, use a hierarchy, nesting one Promise chain in another.
  Uses a closure to capture the intermediate result, `n`, making it available to the nested chain."
  []
  (-> (resolve 5)
      (.then square-and-sum-step)
      (.then prn)))

)
