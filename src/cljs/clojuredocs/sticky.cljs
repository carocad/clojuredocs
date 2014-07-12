(ns clojuredocs.sticky
  (:require [dommy.utils :as utils]
            [dommy.core :as dom]
            [clojure.string :as str])
  (:use-macros [dommy.macros :only [node sel sel1]]))

(defn clog [& args]
  (.log js/console (pr-str args)))

(defn parse-int [s & [default]]
  (try
    (js/parseInt s)
    (catch js/Error e
      (if default
        default
        (throw e)))))

(defn offset-top [$el]
  (loop [y 0
         $el $el]
    (let [parent (.-offsetParent $el)]
      (if-not parent
        y
        (recur
          (+ y (.-offsetTop $el))
          parent)))))

(defn computed-style [$el style-attr]
  (let [attr (name style-attr)
        v (.getPropertyValue
            (.getComputedStyle js/window $el nil)
            attr)]
    (when (and v (string? v))
      (js/parseInt (str/replace v #"px" "")))))

(defn init [$el]
  (let [px-offset (-> $el
                      (dom/attr :data-sticky-offset)
                      (parse-int 100))
        $parent (-> $el dom/ancestor-nodes second)
        _ (prn )
        initial-offset (offset-top $el)
        f (fn [_]
            (if (> (.-pageYOffset js/window) (- initial-offset px-offset))
              (let [{:keys [width]} (-> $el
                                        dom/ancestor-nodes
                                        second
                                        dom/bounding-client-rect)
                    left-padding (computed-style $parent :padding-left)
                    right-padding (computed-style $parent :padding-right)
                    width (- width left-padding right-padding)]
                (dom/add-class! $el :sticky)
                (dom/set-style! $el
                  :width (str width "px")
                  :height (str js/window.innerHeight "px")
                  :top (str px-offset "px")))
              (dom/remove-class! $el :sticky)))]
    (dom/listen! js/window :scroll f)
    (dom/listen! js/window :resize f)))
