(ns com.tddfellow.carshare.main
    (:require [neko.activity :refer [defactivity set-content-view!]]
              [neko.debug :refer [*a]]
              [neko.notify :refer [toast]]
              [neko.resource :as res]
              [neko.find-view :refer [find-view]]
              [neko.threading :refer [on-ui]]
              [neko.ui.mapping :refer [defelement]])
    (:import android.widget.EditText))

;; We execute this function to import all subclasses of R class. This gives us
;; access to all application resources.
(res/import-all)

(defelement :std-button
  :inherits :button
  :attributes {})

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

(defn main-layout [activity]
  [:linear-layout {:orientation :vertical
                   :layout-width :fill
                   :layout-height :wrap}

   [:edit-text {:id ::user-input
                :hint "Type the text here"
                :layout-width :fill}]

   [:std-button {:text "Hello world we will send every text you type to our database!" ;; We use resource here, but could
             ;; have used a plain string too.
              :layout-width :wrap-content
              :layout-height :wrap-content
              :background-resource R$drawable/shinybutton
             :on-click (fn [_] (notify-from-edit activity))}]])

(str R$drawable/shinybutton)

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
