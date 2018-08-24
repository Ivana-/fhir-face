(ns fhir-face.style
  (:require
   [garden.core :as garden]))

(defn style [css] [:style (garden/css css)])

(defn with-common-style [css]
  (style
   (into [:.page
          {;;:width "1000px"
           ;;:margin "0 auto"
           ;;--------------------------------------
           ;;:margin "0 200px"
           ;;:padding-bottom "500px"
           :margin "0 15% 25% 15%"
           :width :auto
           :font-family :fantasy}

          [:.bar {:display :flex
                  :justify-content :space-between
                  :align-items :center
                  :padding-bottom "10px"
                  :border-bottom "1px solid"
                  :border-color :gray
                  :margin-bottom "20px"}]

          #_[:.input {:outline :none
                    :font-size "16px"
                    :font-weight :bold}]

          [:.search {:margin-left "10px"
                     :width "400px"}]
          [:.action {:font-size "20px"
                     :font-weight :bold
                     :cursor :pointer
                     :margin-left "10px"}]
          [:.label {:font-size "20px"
                    :font-weight :bold
                    :margin-left "10px"}]

          [:.btn {:font-size "20px"}]
          [:.footer-actions {:margin-top "20px"
                             :padding-top "20px"
                             :border-top "1px solid"
                             :border-color :gray}]

          ]
         css)))

(def borders
  {:thin-gray {:border "1px solid"
               :border-color "#ddd"}})
