(ns fhir-face.core
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :as rf]
            [fhir-face.routes :as routes]
            [fhir-face.views :as views]
            [fhir-face.config :as config]
            [clojure.string :as str]
            [zframes.cookies :as cookies]
            [zframes.openid :as openid]
            [zframes.window-location :as window-location]
            [zframes.fetch]
            [zframes.redirect :as redirect]
            [re-frisk.core :refer [enable-re-frisk!]]

            ;;[fhir-face.test-svg :as test-svg]

            ))

#_(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
    (swap! app-state update-in [:__figwheel_counter] inc))

(rf/reg-event-db
 ::initialize-db
 (fn [_ _] {}))

#_(rf/reg-event-fx
 ::auth
 (fn [{db :db} [_ params]]
   (let [auth (->> (str/split params #"&")
                   (map #(str/split % #"="))
                   (reduce (fn [a [k v]] (assoc a (keyword k) v)) {}))]
     {:db (assoc db :auth auth)
      :cookies/set {:key :test :value params}})))

(rf/reg-event-fx
 ::initialize
 [(rf/inject-cofx ::openid/jwt :auth)]
 (fn [{db :db jwt :jwt :as cofx} _]
   ;;(prn "========================================= ::initialize")
   ;;(prn cofx)
   ;;(prn (cookies/get-cookie :auth))
   (let [{qs :query-string host :hostname hash :hash url :url :as loc} (window-location/get-location)
         base-url (:base-url qs) ;; "https://cleo-sansara.health-samurai.io"
         openid-url (or (:openid-url qs) (str base-url "/oauth2/authorize"))
         cookie-auth-key (str "auth_" base-url)
         auth (cookies/get-cookie cookie-auth-key)]

     ;; FIXME !!!

     (if ;; false ;;
       (and (nil? jwt) (nil? auth))
       {::redirect/page-redirect
        {:uri openid-url
         :params {:redirect_uri (let [url-items (str/split (.. js/window -location -href) #"#")]
                                  (str (first url-items) "#" (second url-items)))
                  :client_id (or (:client_id qs) "local")
                  :scope "openid profile email"
                  :nonce "ups"
                  :response_type "id_token"}}}
       {;;:dispatch-n [[:route-map/init routes/routes workflow/context-routes]]
        ::cookies/set {:key cookie-auth-key :value (or jwt auth)}
        :db (merge db {:auth (or jwt auth)
                       :config {:base-url base-url
                                :openid-url openid-url}})}))))

(defn dev-setup []
  (when config/debug?
    (enable-re-frisk!)
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (rf/clear-subscription-cache!)
  (reagent/render [views/main-panel] (.getElementById js/document "app")))

(defn ^:export init []
  (routes/app-routes)
  (rf/dispatch-sync [::initialize-db])
  (dev-setup)
  (rf/dispatch [::initialize])
  (mount-root))

