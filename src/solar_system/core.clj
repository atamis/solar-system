(ns solar-system.core
  (:require [taoensso.timbre :as timbre]
            [solar-system.ecs :as ecs])
  (:gen-class))

(timbre/refer-timbre)

(def sys (ref (ecs/create-system)))

(def tick-system (ecs/mapping-system :tick (fn [m] (infof "I've ticked %d times" (:n m)) (update m :n inc))))

(defn add-tick-entity
  [system]
  (let [entity (ecs/create-entity)]
    (-> system
        (ecs/add-entity entity)
        (ecs/add-component entity {:component :tick :n 0}))))

(defn add-systems
  [system]
  (-> system
      (ecs/add-system tick-system)))

(defn tick!
  []
  (dosync
   (alter sys #(ecs/process-tick % 1))))

(defn current-millis [] (java.lang.System/currentTimeMillis))

(defn tick-loop
  [fps]
  (let [frame-time (/ 1000 fps)]
    (loop []
      (let [start (current-millis)
            _ (tick!)
            end (current-millis)
            length (- end start)
            sleeptime (- frame-time length)]
        (infof "Tick loop took %dms, sleeping %dms" (long length) (long sleeptime))
        (Thread/sleep sleeptime)
        (recur)))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (dosync
   (alter sys add-tick-entity)
   (alter sys add-systems))
  (tick-loop 0.5))
