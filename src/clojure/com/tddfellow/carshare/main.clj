(ns com.tddfellow.carshare.main
  (:require [neko.activity :refer [defactivity set-content-view!]]
            [neko.debug :refer [*a]]
            [neko.notify :refer [toast]]
            [neko.resource :as res]
            [neko.log :as log]
            [neko.find-view :refer [find-view]]
            [neko.threading :refer [on-ui]]
            [neko.ui.mapping :refer [defelement]]
            [clojure.data.json :as json])

  (:import android.widget.EditText)
  (:import android.content.Intent)
  (:import android.net.Uri)
  (:import android.location.LocationListener)
  (:import android.location.LocationManager)
  (:import android.content.Context)

  (:import java.net.URL)
  (:import java.net.HttpURLConnection)
  (:import java.io.InputStream)
  (:import java.io.BufferedInputStream)
  (:import java.util.Scanner))

;; We execute this function to import all subclasses of R class. This gives us
;; access to all application resources.
(res/import-all)

(defelement :std-button
  :inherits :button
  :attributes {})

(def cars (agent []))
(def cars-renderer (agent nil))

(def api-endpoint (atom ""))
(def current-location (atom "0,0"))

(def cars-stub [
                 {:description "West Ealing - Hartington Rd"
                  :latitude 51.511318
                  :longitude -0.318178,
                  :distance "3.5 kms"}

                 {:description "Sudbury - Williams Way"
                  :latitude 51.553667
                  :longitude -0.315159
                  :distance "3.6 kms"}

                 {:description "West Ealing - St Leonard’s Rd"
                  :latitude 51.512107
                  :longitude -0.313599
                  :distance "5.0 kms"}
                ])

(declare main-layout)

(def location-listener
  (reify LocationListener
    (onLocationChanged [this location]
      (let [latitude (.getLatitude location)
            longitude (.getLongitude location)
            latlong (str latitude "," longitude)]
        (log/i (str "received location update:" latlong))
        (reset! current-location latlong)))

    (onStatusChanged [this provider status extras] nil)

    (onProviderEnabled [this provider] nil)

    (onProviderDisabled [this provider] nil)))

(defn render-view [activity]
  (on-ui
    (set-content-view! activity (main-layout activity))))

(defn fetch-cars [activity _]
  (let [url (new URL (str
                       @api-endpoint
                       "/cars?location="
                       @current-location
                       "&units=kms&limit=4"))
        ^HttpURLConnection connection (.openConnection url)]

    (try
      (let [^InputStream stream (new BufferedInputStream (.getInputStream connection))
            scanner (.useDelimiter (new Scanner stream "UTF-8") "\\A")
            raw (if (.hasNext scanner) (.next scanner) "{}")
            data (json/read-str raw :key-fn keyword)
            cars (get data :cars [])]
        (send-off cars-renderer
                  (fn [_] (render-view activity) nil))
        cars)

      (finally (.disconnect connection)))))

(defn grab-text [activity id]
  (str (.getText (find-view activity id))))

(defn view-to-atom [activity id x]
  (reset! x (grab-text activity id)))

(defn reload-cars
  [activity]
  (view-to-atom activity ::api-endpoint api-endpoint)
  (send-off cars (partial fetch-cars activity)))

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

(defn link-color [] (android.graphics.Color/rgb 0x00 0x00 0x99))

(defn link-view [opts on-click]
  (text-view (merge {:text-color (link-color)
                     :on-click (fn [_] (on-click))}
                    opts)))

(defn link-with-tooltip [opts on-click tooltip tooltip-opts]
  (-layout :vertical
           {:layout-width :wrap}
           [(link-view (merge {:padding-bottom 0}
                              opts)
                       on-click)
            (text-view (merge {:text tooltip
                               :text-size 8
                               :padding-top 0}
                              tooltip-opts))]))

(defn car-distance [{:keys [:distance]}]
  (text-view {:text (str distance)}))

(defn car-description [car {:keys [:on-click]}]
  (let [{:keys [:description]} car]
    (-layout :vertical
             {:layout-width :wrap}
             [(link-with-tooltip {:text description}
                                 #(on-click car)
                                 "(click to open in maps)"
                                 {})
              (car-distance car)])))

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

(defn receive-location-updates [activity]
  (let [^LocationManager location-manager (.getSystemService activity Context/LOCATION_SERVICE)
        ^LocationListener listener location-listener]
    (.requestLocationUpdates
      location-manager
      LocationManager/GPS_PROVIDER
      0
      0.0
      listener)))

(defn main-layout [activity]
  (vertical-layout

    (text-view {:text "Closest cars:"
                :padding 10})

    (apply vertical-layout
           (car-list @cars
                     {:on-click (partial open-car activity)}))

    [:edit-text {:id ::api-endpoint
                 :text @api-endpoint
                 :hint "API endpoint (e.g.: http://example.org:4567)"}]

    [:std-button {:text "Reload"
                  :layout-width :wrap-content
                  :layout-height :wrap-content
                  :on-click (fn [_] (reload-cars activity))}]))

;; This is how an Activity is defined. We create one and specify its onCreate
;; method. Inside we create a user interface that consists of an edit and a
;; button. We also give set callback to the button.
(defactivity com.tddfellow.carshare.MainActivity
  :key :main

  (onCreate [this bundle]
            (.superOnCreate this bundle)
            (neko.debug/keep-screen-on this)
            (receive-location-updates this)

            ;; eval to do a live update update
            (render-view (*a))))

(defn- -dev-update-ui []
  (render-view (*a)))
