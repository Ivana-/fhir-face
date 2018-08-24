(ns zframes.openid
  (:require
   [clojure.spec.alpha :as s]
   [re-frame.core :as rf]
   [clojure.string :as str]
   [goog.crypt.base64 :refer [encodeString decodeString]]))

(defn ^:export decode
  [token]
  (let [segments (s/conform (s/cat :header string? :payload string? :signature string?)
                            (str/split token "."))]
    (if-not (map? segments)
      (throw (js/Error. "invalid token"))
      (let [header (.parse js/JSON (js/atob (:header segments)))
            payload (.parse js/JSON (js/atob (:payload segments)))]
        payload))))

(defn check-token []
  (let [hash (when-let [h (.. js/window -location -hash)] (str/replace h  #"^#" ""))]
    (when (str/index-of hash "id_token")
      (let [token (->> (str/split hash "&")
                       (filter #(str/starts-with? % "id_token="))
                       (map (fn [x] (str/replace x #"^id_token=" "")))
                       (first))
            jwt (js->clj (decode token) :keywordize-keys true)]
        (set! (.. js/window -location -hash) (or (first (str/split hash "#")) ""))
        (assoc jwt :id_token token)))))

(rf/reg-cofx
 ::jwt
 (fn [coeffects]
   (assoc-in coeffects [:jwt] (check-token))))
