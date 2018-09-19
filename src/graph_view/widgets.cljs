(ns graph-view.widgets)

(defn input-range [{:keys [state path] :as params}]
  [:input (merge {:type "range"
                  :min 1
                  :max 100
                  :step 1
                  :class :input
                  :value (get-in @state path)
                  :on-change #(swap! state assoc-in path (js/parseFloat (.. % -target -value)))}
                 (dissoc params :state :path))])

(defn input-checkbox [{:keys [state path] :as params}]
  [:input (merge {:type "checkbox"
                  :class :input
                  :checked (= "true" (str (get-in @state path)))
                  :on-change #(swap! state assoc-in path (.. % -target -checked))}
                 (dissoc params :state :path))])

(defn input-integer [{:keys [state path] :as params}]
  [:input (merge {:type "number"
                  :class :input
                  ;;:step "1"
                  :value (str (get-in @state path))
                  :on-change #(swap! state assoc-in path (js/parseFloat (.. % -target -value)))}
                 (dissoc params :state :path))])

(defn input-textarea [{:keys [state path] :as params}]
  [:textarea (merge {:class :input
                     :value (str (get-in @state path))
                     :on-change #(swap! state assoc-in path (.. % -target -value))}
                    (dissoc params :state :path))])
