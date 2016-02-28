(ns com.tddfellow.carshare.main
  (:require [neko.activity :refer [defactivity set-content-view!]]
            [neko.debug :refer [*a]]
            [neko.notify :refer [toast]]
            [neko.resource :as res]
            [neko.find-view :refer [find-view]]
            [neko.threading :refer [on-ui]]
            [neko.ui.mapping :refer [defelement]])
  (:import android.widget.EditText)
  (:import android.content.Intent)
  (:import android.net.Uri))

;; We execute this function to import all subclasses of R class. This gives us
;; access to all application resources.
(res/import-all)

(defelement :std-button
  :inherits :button
  :attributes {})

(def cars (atom [
                 {:description "West Ealing - Hartington Rd"
                  :latitude 51.511318
                  :longitude -0.318178}

                 {:description "Sudbury - Williams Way"
                  :latitude 51.553667
                  :longitude -0.315159}

                 {:description "West Ealing - St Leonard’s Rd"
                  :latitude 51.512107
                  :longitude -0.313599}
                 ]))

(defn notify-from-edit
  "Finds an EditText element with ID ::user-input in the given activity. Gets
  its contents and displays them in a toast if they aren't empty. We use
  resources declared in res/values/strings.xml."
  [activity]
  (let [^EditText input (.getText (find-view activity ::user-input))]
    (toast (if (empty? input)
             (res/get-string R$string/input_is_empty)
             (res/get-string R$string/your_input_fmt input))
           :long)))

(defn -layout [kind opts elems]
  (into
    [:linear-layout (merge {:orientation kind
                            :layout-width :fill
                            :layout-height :wrap}
                           opts)]
    elems))

(defn vertical-layout [& elems]
  (-layout :vertical {} elems))

(defn horizontal-layout [& elems]
  (-layout :horizontal {} elems))

(defn text-view [opts]
  [:text-view (merge {:padding 10}
                     opts)])

(def link-color (android.graphics.Color/rgb 0x00 0x00 0x99))

(defn car-description [car {:keys [:on-click]}]
  (let [{:keys [:description]} car]
    (-layout :vertical
             {:layout-width :wrap}
             [(text-view {:text description
                          :text-color link-color
                          :padding-bottom 0
                          :on-click (fn [_] (on-click car))})
              (text-view {:text "(click to open in maps)"
                          :text-size 8
                          :padding-top 0})])))

(defn car-latitude [{:keys [:latitude]}]
  (text-view {:text (str latitude)
              :layout-gravity :right}))

(defn car-longitude [{:keys [:longitude]}]
  (text-view {:text (str longitude)
              :layout-gravity :right}))

(defn car-view [car opts]
  (horizontal-layout
    (car-description car opts)
    (vertical-layout
      (car-latitude car)
      (car-longitude car))))

(defn car-list [cars opts]
  (for [car cars]
    (car-view car opts)))

(defn open-car [activity {:keys [:description :latitude :longitude]}]
  (let [label description
        uri-start "https://www.google.com/maps/"
        query (str latitude "," longitude " (" label ")")
        encoded-query (Uri/encode query)
        place (str "@" latitude "," longitude
                "?q=" encoded-query)
        uri-full (str uri-start place)
        uri (Uri/parse uri-full)
        intent (new Intent Intent/ACTION_VIEW uri)]
    (.startActivity activity intent)))

(defn main-layout [activity]
  (vertical-layout

    (text-view {:text "Closest cars:"
                :padding 10})

    (apply vertical-layout
           (car-list @cars
                     {:on-click (partial open-car activity)}))

    [:std-button {:text "Reload"
                  :layout-width :wrap-content
                  :layout-height :wrap-content
                  :on-click (fn [_] (notify-from-edit activity))}]))

;; This is how an Activity is defined. We create one and specify its onCreate
;; method. Inside we create a user interface that consists of an edit and a
;; button. We also give set callback to the button.
(defactivity com.tddfellow.carshare.MainActivity
  :key :main

  (onCreate [this bundle]
            (.superOnCreate this bundle)
            (neko.debug/keep-screen-on this)

            ;; eval to do a live update update
            (on-ui
              (set-content-view! (*a) (main-layout (*a))))))

(defn- -dev-update-ui []
  (on-ui
    (set-content-view! (*a) (main-layout (*a)))))
