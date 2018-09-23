(ns graph-view.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as str]
            [fhir-face.style :as style]
            [fhir-face.nav :as nav]
            [graph-view.widgets :as ws]
            [fhir-face.model :as model]))

(def style
  [:.content {:display "flex"
              :height :-webkit-fill-available
              :padding "5px"}
   ;; [:.input (merge (:thin-gray style/borders)
   ;;                 {:outline :none
   ;;                  :font-size "16px"
   ;;                  :font-weight :bold})]


   ;; [:table {:width "100%"
   ;;          :border-collapse :collapse}]
   ;; [:th {:color "#ddd"}]
   ;; [:td {:font-family :system-ui
   ;;       :font-size "20px"
   ;;       :height "50px"
   ;;       :cursor :pointer
   ;;       :border-bottom "1px solid #ddd"}]
   ;; ;;[:td:hover {:background-color "#f5f5f5"}]
   ;; [:.item:hover {:background-color "#f5f5f5"}]

   ;; ;;[:.id :.display {:text-align :left}]
   ;; [:.display {:padding-left "20px"}]
   ;; [:.size {:padding-left "20px"
   ;;          :text-align :right}]

   [:.bar {:display "inline-flex"
           :flex-direction "column"
           :margin "0 10px 0 65px"}]

   [:.svg-container {:width "100%"
                     ;;:background-color :black
                     ;;:border "10px solid"
                     }]

   [:.field {;;:border "1px solid #ddd"
             ;;:background-color :black
             }]

   [:.draggable {:cursor :move}]
   [:.vertex-name {:font-family :system-ui
                   ;;:font-size "10px"
                   :color :gray
                   :cursor :pointer
                   }]
   [:.vertex-menu {:z-index 1
                 :position :absolute
                 ;;:min-width "100%"
                 ;;:width :max-content
                 :background-color "#ffffff"}]

   [:.btn-group {:display :flex}]

   [:.btn {:font-size "20px"
           :cursor :pointer
           :background-color :lightgray
           :border :none
           :outline :none
           :flex-grow 1
           :width "100px"}
    ]
   [:.red {:background-color "#cc0000"}]
   [:.green {:background-color "#009900"}]


   [:.splitter {:height "20px"}]

   [:.input {:outline :none
             :font-size "16px"
             ;;:font-weight :bold
             ;;:color :honeydew
             ;;:background-color :black
             :border "1px solid #ccc"
             ;;:margin-top "5px"
             }]

   [:.label {:margin "10px 5px 0 0"
             ;;:color :gray
             }]

   [:.vertexes-names {:display :flex
                      :align-items :flex-end}]

   [:.vertexes-edges {:display :flex
                      :align-items :baseline}
    [:.input {:width "45px"
              :height :fit-conten}]]

   #_[:.edges-list {:resize :none
                  :flex-grow 1
                  :spell-check false}]

   [:.vertex-list {:height "300px"
                    :flex-grow 1
                    :display :flex
                       :flex-direction :column
                   :margin-top "10px"
                       :border "1px solid gray"
                       :overflow-y :auto}]

   [:.menu-item {:font-family :system-ui
                 :padding "2px 5px"
                 :cursor :pointer}
    [:&:hover {;;:background-color "#f1f1f1"
               :text-decoration :underline}]]

   [:.added {:background-color :gainsboro}]
   [:.not-added {:background-color :white}]


   ])


(defn p2 [x] (* x x))

(defn close-to [x x*] (< -1E-15 (- x* x) 1E-15))

(defn in-diap [x from to] (min to (max from x)))

(defn norm-by-abs [x limit] (min limit (max (- limit) x)))


(defn out-of-limit [x limit] x #_(if (>= x 0) (max limit x) (min (- limit) x)))

(defn smooth-slide [x] (if-not x 0 (let [v (/ x 100)] (* v v))))


(defn fo [id  {x :x y :y edges :edges}
          id* {x* :x y* :y edges* :edges}
          {:keys [repulsive-force coupling-stiffness-factor relative-edge-length]}]

  (if (and (close-to x x*) (close-to y y*))
    [0 0]
    (let [k-rep-f  (* 0.0001   (smooth-slide repulsive-force))
          k-stiff  (* 0.3      (smooth-slide coupling-stiffness-factor))
          edge-len (* 0.5      (smooth-slide relative-edge-length))

          edge? (or (contains? edges id*) (contains? edges* id))
          l2 (+
              ;;0.000001
              (p2 (- x x*)) (p2 (- y y*)))
          l (Math/sqrt l2)

          fv (- (/ 1 l2))
          fe (if edge? (- l edge-len) 0)

          ;;f (+ (* 0.0001 fv) (* 0.1 fe))
          f (+ (* k-rep-f fv) (* k-stiff fe))

          k (/ f l)

          fx (* k (out-of-limit (- x* x) 1E-10))
          fy (* k (out-of-limit (- y* y) 1E-10))

          ;; ke (/ fe l)
          ;; fex (* ke (- x* x))
          ;; fey (* ke (- y* y))


          ;; kD  0 ;;-0.002

          ;; dfx (* kD (norm-by-abs (- fx (or fvx 0)) 0.1))
          ;; dfy (* kD (norm-by-abs (- fy (or fvy 0)) 0.1))

          ;; kI 0 ;;0.00002
          ;; fix* (* kI (norm-by-abs (+ fex (or fix 0)) 100))
          ;; fiy* (* kI (norm-by-abs (+ fey (or fiy 0)) 100))
          ]
      [(+ fx
          ;; dfx fix*
          ) (+ fy
               ;; dfy fiy*
               )]
      )))

(defn foall [id {:keys [x y fix fiy] :as p} vs params]
  (let [
         [dx* dy*] (reduce (fn [[xa ya] [idv v]]
                             (let [[xi yi] (fo id p idv v params)] [(+ xa xi) (+ ya yi)]))
                           [0 0] vs)

         [dx dy] (reduce (fn [[xa ya] v]
                             (let [[xi yi] (fo id p nil v params)] [(+ xa xi) (+ ya yi)]))
                         [dx* dy*] [{:x x :y 0} {:x x :y 1} {:x 0 :y y} {:x 1 :y y}])

         dx (norm-by-abs dx 0.05)
         dy (norm-by-abs dy 0.05)

         fx (in-diap (+ x dx) 0.001 0.999)
         fy (in-diap (+ y dy) 0.001 0.999)
         ]

    ;;(prn p (->> ps (mapv #(fo p %))) dx dy)

    {:x fx :y fy
     ;; :fvx dx :fvy dy
     ;; :fix (+ dx (or fix 0)) :fiy (+ dy (or fiy 0))
     }))






(defn on-tik [state]
  (let [db @state
        {vs :data} db
        params (select-keys db [:repulsive-force :coupling-stiffness-factor :relative-edge-length])
        r (reduce (fn [a [id v]] (assoc a id (merge v (if (= id (get-in db [:dragging-id]))
                                                        {:x (:x v) :y (:y v)}
                                                        (foall id v vs params))))) {} vs)]
    (swap! state assoc :data r)
    (:simulation-on @state)))

(defn periodic [f v]
  (-> (js/Promise. (fn [resolve] (js/setTimeout #(resolve (f v)) 100)))
      (.then #(if % (periodic f v)))
      (.catch prn)))

(defn stop-go [state]
  (if-not (:simulation-on @state) (periodic on-tik state))
  (swap! state update :simulation-on not))



(defn read-graph-from-string [s]
  (let [ft (->> s
                str/split-lines
                (remove str/blank?)
                (map (fn [e] (let [[f t & _] (-> e str/trim (str/split #"\s"))] {:f f :t t}))))
        vertex (->> ft
                    (reduce (fn [a {:keys [f t]}] (cond-> a
                                                    f (conj f)
                                                    t (conj t))) #{})
                    (reduce (fn [a x] (conj a
                                            {:x (Math/random) ;;(+ 0.5 (* 1E-10 (Math/random)))
                                             :y (Math/random) ;;(+ 0.5 (* 1E-10 (Math/random)))
                                             :id (str x)})) []))
        edges (reduce (fn [a {:keys [f t]}] (if-not t a (assoc a f (conj (get a f #{}) t)))) {} ft)]
    (reduce (fn [a {id :id :as v}] (assoc a id
                                          (-> v
                                              (dissoc :id)
                                              (assoc :edges (edges id))))) {} vertex)))

(defn user-data [state]
  (let [r (read-graph-from-string (:input-graph @state))]
    (swap! state assoc
           :data r
           :vertexes-amount (count r)
           :edges-amount (reduce (fn [a [_ {es :edges}]] (+ a (count es))) 0 r))))

(defn random-data [state]
  (let [db @state
        n (:vertexes-amount db)
        e (:edges-amount db)
        max-e (quot (* n (- n 1)) 2)]
    (if (> e max-e)
      (js/alert (str "Amount of edges can not be greater than v*(v-1)/2 = " max-e))
      (let [coin-p-q (fn [p q] (cond (= 0 p) false (= p q) true :else (< (* q (Math/random)) p)))
            [r vs] (loop [p e, q max-e, f 1, t 2, r "", r-size 0, vs #{}]
                     (if (= e r-size)
                       [r vs]
                       (let [[f* t*] (if (> (inc t) n) [(inc f) (+ 2 f)] [f (inc t)])]
                         (if (coin-p-q p q)
                           (recur (dec p) (dec q) f* t* (str r f " " t "\n") (inc r-size) (conj vs f t))
                           (recur p       (dec q) f* t* r                    r-size       vs)))))
            s (str r (->> (range 1 (inc n))
                          (remove #(contains? vs %))
                          (str/join "\n")))]
        (swap! state assoc
               :input-graph s
               :data (read-graph-from-string s))))))


(def to-color "red")
(def from-color "blue")

(defn create-vertex-menu [data state id]
  ;;(js/alert id)
  (let [graph (:references-graph data)
         to (sort (get-in graph [id :edges]))
        from (sort (reduce (fn [a [k {e :edges}]] (if-not (contains? e id) a (conj a k))) [] graph))

        add-vertex (fn [id]

                    (if (get-in @state [:data id])
                      (swap! state update :data #(dissoc % id))
                      (swap! state update :data
                             #(assoc % id (assoc (get (:references-graph data) id)
                                                 :x (Math/random) :y (Math/random))))

                      ))


        ]


    [:div
     (if-not (empty? to) [:div.menu-group.to
      {:style {:border (str "1px solid " to-color)
               :margin-bottom "5px"}}
      (doall (map (fn [id] [:div.menu-item {:key id
                          :class (if (get-in @state [:data id]) :added :not-added)
                          :on-click #(add-vertex id)} id]) to))
      ])

     (if-not (empty? from) [:div.menu-group.from
      {:style {:border (str "1px solid " from-color)
               :margin-bottom "5px"}}
      (doall (map (fn [id] [:div.menu-item {:key id
                           :class (if (get-in @state [:data id]) :added :not-added)
                           :on-click #(add-vertex id)} id]) from))
      ])]

))


(defn area-component [{state :state rf-data :data}]
  (let [handle-mouse-move (fn mouse-move [e]
                            (let [{:keys [dragging-id dx dy area-node area-width area-height]} @state
                                  area-bounds (.getBoundingClientRect area-node)
                                  x (/ (- (.-clientX e) (.-left area-bounds) (.-clientLeft area-node) dx) area-width)
                                  y (/ (- (.-clientY e) (.-top area-bounds)  (.-clientTop area-node)  dy) area-height)]
                              (if dragging-id
                                (swap! state #(-> %
                                               (assoc-in [:data dragging-id :x] x)
                                               (assoc-in [:data dragging-id :y] y))))))
        handle-mouse-up  (fn mouse-up [e]
                           ;;(prn "up")
                           (swap! state dissoc :dragging-id)
                           (.removeEventListener js/document "mousemove" handle-mouse-move)
                           (.removeEventListener js/document "mouseup" mouse-up))

        handle-mouse-down  (fn [e]
                             ;;(prn "down")
                             (.removeAllRanges (.getSelection js/window))
                             (let [bounds (-> e .-target .getBoundingClientRect)
                                   dx (- (.-clientX e) (/ (+ (.-left bounds) (.-right bounds)) 2))
                                   dy (- (.-clientY e) (/ (+ (.-top bounds) (.-bottom bounds)) 2))]
                               (swap! state assoc
                                      :dragging-id (-> e .-currentTarget (.getAttribute "data-id")) :dx dx :dy dy)
                               (.addEventListener js/document "mousemove" handle-mouse-move)
                               (.addEventListener js/document "mouseup" handle-mouse-up)))

        norm (fn [x limit] (* x limit))]

    (r/create-class
     {
       :component-did-mount (fn [this]
                              (let [root (r/dom-node this)
                                    bounds (.getBoundingClientRect root)]
                                (swap! state assoc
                                       :area-node root
                                       :area-width  (- (.-right bounds)  (.-left bounds) 200)
                                       :area-height (- (.-bottom bounds) (.-top bounds)))))

       ;;:component-will-unmount (fn [this]
       ;;                          (.removeEventListener js/document "mousemove" handle-mouse-move)
       ;;                          (.removeEventListener js/document "mouseup" handle-mouse-up))

       :reagent-render (fn [{state :state rf-data :data}]
                         (let [{:keys [area-width area-height data show-vertex-names vertex-menu]} @state]
                           [:div.svg-container
                            [:svg.field
                             {:width "100%" :height "100%"}
                             ;;{:viewBox "0 0 1 1"}

                             (into [:defs]
                              (for [[id v] data
                                    e (:edges v)
                                    :let [vt (get data e)]
                                    :when vt]
                                (let [id-gr (str id "-" e)
                                       x1 (norm (:x v) area-width)
                                      y1 (norm (:y v) area-height)
                                      x2 (norm (:x vt) area-width)
                                      y2 (norm (:y vt) area-height)]
                                  ^{:key (gensym)}
                                  [:linearGradient {:id id-gr :x1 x1 :y1 y1 :x2 x2 :y2 y2 :gradientUnits "userSpaceOnUse"}
                                   [:stop {:stop-color to-color :offset "20%"}]
                                   [:stop {:stop-color from-color :offset "80%"}]])))

                             (for [[id v] data
                                   e (:edges v)
                                   :let [vt (get data e)]
                                   :when vt]
                                 (let [id-gr (str id "-" e)
                                        x1 (norm (:x v) area-width)
                                       y1 (norm (:y v) area-height)
                                       x2 (norm (:x vt) area-width)
                                       y2 (norm (:y vt) area-height)]
                                   ^{:key (gensym)}
                                   [:line {:x1 x1 :y1 y1 :x2 x2 :y2 y2 :stroke (str "url(#" id-gr ")") :stroke-width 1}]))

                             (for [[id v] data]
                               ^{:key id}
                               [:circle.draggable {:cx (norm (:x v) area-width) :cy (norm (:y v) area-height)
                                                   :r 8 :stroke "#444" :stroke-width 6 :fill "aliceblue"
                                                   :onMouseDown handle-mouse-down
                                                   :data-id id
                                                   :on-drag-start (fn [ev] false)}])

                             (if true ;;show-vertex-names
                               (for [[id v] data]
                                 (let [x (+ 12 (norm (:x v) area-width))
                                       y (+ 12 (norm (:y v) area-height))]
                                   ^{:key id}
                                 [:text.vertex-name {:x x :y y
                                                     ;;:stroke "yellow"
                                                     :on-click (fn [e]

                                                                   ;;(prn node)
                                                                   (swap! state assoc
                                                                 :vertex-menu {:id id
                                                                               :x (- (.-clientX e) 10)
                                                                               :y (- (.-clientY e) 10)
                                                                               }))} id])))
                             ]
                            (if vertex-menu [:div.vertex-menu
                                             {:style {:left (str (:x vertex-menu) "px")
                                                      :top (str (:y vertex-menu) "px")}
                                              :on-mouse-leave #(swap! state dissoc :vertex-menu)

                                              }
                                             [create-vertex-menu rf-data state (:id vertex-menu)]])
                            ]))})))

(defn rf-data [data state]
  (let [rg (:references-graph data)
        vx-from-edges (reduce (fn [a [k {e :edges}]] (into a e)) #{} rg)
        graph (as-> rg $
                    ;;(reduce (fn [a {id :id}] (if (get a id) a (assoc a id {}))) $ (:entity data))
                    (reduce (fn [a id] (if (get a id) a (assoc a id {}))) $ vx-from-edges)
                    (reduce (fn [a [k v]] (assoc a k (assoc v :x (Math/random) :y (Math/random)))) {} $))]
    (swap! state assoc
           :data graph
           ;;:vertexes-amount (count r)
           ;;:edges-amount (reduce (fn [a [_ {es :edges}]] (+ a (count es))) 0 r)
           )))

(defn main-page [params]
  ;;(prn "main-page" params)
  (let [state (r/atom {:repulsive-force 80
                       :coupling-stiffness-factor 25
                       :relative-edge-length 90
                       ;;:show-vertex-names true
                       ;;:vertexes-amount 30
                       ;;:edges-amount 30
                       ;;:input-graph ""
                       :data {}})]
    ;;(random-data state)
    ;;(user-data state)

    (fn [params]
      ;;(prn "main-page-fn" params)
      (let [data @(rf/subscribe [::model/data])]
        (cond
          (:is-fetching data) [:div.loader "Loading"]

          (:error data) [:div (str (:error data))]

          :else [:div.page
                 [style/style style]
                 [nav/nav-bar]
                 [:div.content
                  ;;[:div (str @state)]
                  [:span.bar
                   [:div.btn-group
                    [:button.btn {:on-click #(stop-go state)
                                  :class (if (:simulation-on @state) :red :green)} (if (:simulation-on @state) "Stop" "Go!")]
                    [:button.btn {:on-click #(swap! state assoc :data {})} "Clear"]]
                   [:label.label "repulsive force"]
                   [ws/input-range {:state state
                                    :path [:repulsive-force]}]
                   [:label.label "coupling stiffness factor"]
                   [ws/input-range {:state state
                                    :path [:coupling-stiffness-factor]}]
                   [:label.label "relative edge length"]
                   [ws/input-range {:state state
                                    :min 10
                                    :path [:relative-edge-length]}]
                   #_[:div.vertexes-names
                      [:label.label "show vertex names"]
                      [ws/input-checkbox {:state state
                                          :path [:show-vertex-names]}]]
                   #_[:div.splitter]
                   #_[:button.btn {:on-click #(rf-data data state)} "RF data"]
                   #_[:button.btn {:on-click #(random-data state)} "Random data"]
                   #_[:div.vertexes-edges
                      [:label.label "vertices"]
                      [ws/input-integer {:state state
                                         :path [:vertexes-amount]
                                         :class "input integer"}]
                      [:label.label ""]
                      [:label.label "edges"]
                      [ws/input-integer {:state state
                                         :path [:edges-amount]
                                         :class "input integer"}]]
                   #_[:div.splitter]
                   #_[:button.btn {:on-click #(user-data state)} "Custom data"]
                   #_[ws/input-textarea {:state state
                                         :path [:input-graph]
                                         :class "input edges-list"}]
                   [:div.vertex-list
                    (doall (map-indexed (fn [i x]
                                          (let [id (:id x)]
                                            [:div.menu-item {:key i
                                                             :on-click (fn []
                                                                         (if (get-in @state [:data id])
                                                                           (swap! state update :data #(dissoc % id))
                                                                           (swap! state update :data
                                                                                  #(assoc % id (assoc (get (:references-graph data) id)
                                                                                                      :x (Math/random) :y (Math/random))))))
                                                             :class (if (get-in @state [:data id]) :added :not-added)}
                                             (str id " " (count (get-in data [:references-graph id :edges])))])) (:entity data)))]
                   ]
                  [area-component {:state state :data data}]]])))))

(def routes {:graph-view (fn [params]
                               ;;(prn "graph-view --------------------------------------" params)
                               (rf/dispatch [::model/load-all params])
                               [main-page (:query-params params)])})
