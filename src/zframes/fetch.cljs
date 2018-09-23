(ns zframes.fetch
  (:require [re-frame.core :as rf]
            [clojure.string :as str]))

(defn to-query [params]
  (->> params
       (mapv (fn [[k v]] (str (name k) "=" v)))
       (str/join "&")))

#_(rf/reg-sub
 :xhr/url
 (fn [db [_ url]]
   (str (get-in db [:config :base-url]) url)))

#_(comment
(defn json-fetch [{:keys [uri token headers params success error] :as opts}]
  (let [headers (merge (or headers {})
                       {"Accept" "application/json"
                        "content-type" "application/json"
                        "Authorization" (str "Bearer " token)})
        fetch-opts (-> (merge {:method "get" :mode "cors"} opts)
                       (dissoc :uri :headers :success :error :params)
                       (assoc :headers headers))
        fetch-opts (if (:body opts)
                     (assoc fetch-opts :body (.stringify js/JSON (clj->js (:body opts))))
                     fetch-opts)
        url uri]
    (->
     (js/fetch (str url (when params (str "?" (to-query params))))
               (clj->js fetch-opts))
     (.then
      (fn [resp]
        (.then (.json resp)
               (fn [doc]
                 (let [data (js->clj doc :keywordize-keys true)
                       event-params {:request opts
                                     :response resp
                                     :data data}]
                   (if (< (.-status resp) 299)
                     ;; if success/error are maps - calling dispatch events, else calling as functions
                     (cond
                       (map? success) (rf/dispatch [(:event success) (merge success event-params)])
                       success (success data))
                     (cond
                       (map? error)   (rf/dispatch [(:event error)   (merge error   event-params)])
                       error (error data)))))))))))

(defn json-fetch* [x]
  (cond (map? x) (json-fetch x)
        (vector? x) (doseq [i x] (json-fetch i))))

(rf/reg-fx :json/fetch json-fetch*)
)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Via promices
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn fetch-promise [{:keys [uri token headers params] :as opts}]
  (let [headers (merge (or headers {})
                       {"Accept" "application/json"
                        "content-type" "application/json"

                        ;; FIXME return on sansara servers
                        ;;"Authorization" (str "Bearer " token)

                        })
        fetch-opts (-> (merge {:method "get" :mode "cors"} opts)
                       (dissoc :uri :headers :params)
                       (assoc :headers headers))
        fetch-opts (if (:body opts)
                     (assoc fetch-opts :body (.stringify js/JSON (clj->js (:body opts))))
                     fetch-opts)
        url uri]
    (->
     (js/fetch (str url (when params (str "?" (to-query params))))
               (clj->js fetch-opts))
     #_(.then
      (fn [resp]
        (.then (.json resp)
               (fn [doc]
                 (let [data (js->clj doc :keywordize-keys true)
                       res {:request opts
                            :response resp
                            :data data}]
                   (if (> (.-status resp) 299)
                     (let [e (js/Error. (str "Failed to fetch " uri))]
                       (aset e "params" res)
                       (throw e))
                     (js/Promise.resolve res)))))
        ))

     (.then (fn [resp] (js/Promise.all [resp (.json resp)])))

     (.then (fn [[resp doc]] (let [data (js->clj doc :keywordize-keys true)
                                   res {:request opts
                                        :response resp
                                        :data data}]
                               (if (> (.-status resp) 299)
                                 (let [e (js/Error. (str "Failed to fetch " uri))]
                                   (aset e "params" res)
                                   (throw e))
                                 (js/Promise.resolve res)))))

     ;; (.catch (fn [e] (throw (js/Error. (str "failed to fetch " uri)))))
     )))

(defn error-data [e] (.-params e))
(defn error-message [e] (.-message e))

