(ns fhir-face.model
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [zframes.redirect :refer [href redirect]]))

(def root-path [:main])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Loader
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


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

