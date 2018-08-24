(ns zframes.redirect
  (:require [re-frame.core :as rf]
            [zframes.window-location :as window-location]
            [clojure.string :as str]))

(defn page-redirect [url]
  (set! (.-href (.-location js/window)) url))

(defn redirect [url]
  (set! (.-hash (.-location js/window)) url))

#_(defn redirect [url]
  (aset (.-location js/window) "hash" url))

#_(rf/reg-fx
 ::redirect
 (fn [opts]
   (redirect (str (:uri opts)
                  (when-let [params (:params opts)]
                    (window-location/gen-query-string params))))))
#_(rf/reg-event-fx
 ::redirect
 (fn [fx [_ opts]]
   {::redirect opts}))

(rf/reg-fx
 ::page-redirect
 (fn [opts]
   (page-redirect (str (:uri opts)
                       (when-let [params (:params opts)]
                         (->> params
                              (map (fn [[k v]] (str (name k) "=" (js/encodeURIComponent v))))
                              (str/join "&")
                              (str "?")))))))

#_(rf/reg-fx
 ::set-query-string
 (fn [params]
   (let [loc (.. js/window -location)]
     (.pushState
      js/history
      #js{} (:title params)
      (str (window-location/gen-query-string (dissoc params :title)) (.-hash loc)))
     (zframes.routing/dispatch-context nil))))

#_(rf/reg-event-fx
 ::merge-params
 (fn [{db :db} [_ params]]
   (let [pth (get db :fragment-path)
         nil-keys (reduce (fn [acc [k v]]
                            (if (nil? v) (conj acc k) acc)) [] params)
         old-params (or (get-in db [:fragment-params :params]) {})]
     {::redirect {:uri pth
                  :params (apply dissoc (merge old-params params)
                                 nil-keys)}})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#_(defn to-query-params [params]
  (->> params
       (map (fn [[k v]] (str (name k) "=" v)))
       (str/join "&")))

(defn href [& parts]
  (let [params (if (map? (last parts)) (last parts) nil)
        parts (if params (butlast parts) parts)
        url (str "/" (str/join "/" (map (fn [x] (if (keyword? x) (name x) (str x))) parts)))]
    #_(when-not  (route-map/match [:. url] routes)
      (println (str url " is not matches routes")))
    (str "#" url (if-not (empty? params) (window-location/gen-query-string params)))))


#_(defn parse-query-string [s]
  (let [[uri ps] (str/split s #"\?")]
    (cond-> {} ;;{:uri uri}
      ps (merge (reduce (fn [a x]
                          (let [[k v] (str/split x #"\=")]
                            (assoc a (keyword k) v))) {} (str/split ps #"\&"))))))

#_(defn route-params [] (parse-query-string (-> js/window .-location .-href)))
