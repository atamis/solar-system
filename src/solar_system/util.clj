(ns solar-system.util)

; The array of methods returned by .getDeclaredMethods seems always sorted.
(defn arity-min
  [fun]
  (->> fun
       class
       .getDeclaredMethods
       first
       .getParameterTypes
       alength))

(defn arity-max
  [fun]
  (->> fun
       class
       .getDeclaredMethods
       last
       .getParameterTypes
       alength))

(defn arity-all
  [fun]
  (->> fun
       class
       .getDeclaredMethods
       (map #(alength (.getParameterTypes %1)))))
