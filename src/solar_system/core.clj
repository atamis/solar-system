(ns solar-system.core
  (:require [taoensso.timbre :as timbre]
            [betrayer.ecs :as ecs]
            [betrayer.system :as system]
            [clojure.core.async :as async]
            [com.rpl.specter :as sp])
  (:gen-class))

(timbre/refer-timbre)

(defn container-take
  [comp item amount]
  (sp/replace-in [item]
                 (fn [orig-amt]
                   (let [taken (if (< orig-amt amount) orig-amt amount)]
                     [(- orig-amt taken) taken]))

                 comp
                 :merge-fn (fn [curr new] new)))

(defn container-put
  [comp item amount]
  (sp/transform [item]
                #(if %1 (+ %1 amount) amount)
                comp))

(def sys (ref (ecs/create-world)))

(defn add-tick-entity
  [system]
  (let [entity (ecs/create-entity)]
    (-> system
        (ecs/add-entity entity)
        (ecs/add-component entity :tick 0))))

(defn add-miner-entity
  [world]
  (let [entity (ecs/create-entity)]
    (-> world
        (ecs/add-entity entity)
        (ecs/add-component entity :debug true)
        (ecs/add-component entity :container {})
        (ecs/add-component entity :miner :iron))))

(def miner-system (system/iterating-system
                   :miner
                   (fn [entity]
                     (ecs/update-component :container #(container-put %1 (ecs/get-component :miner) 1)))))

(def tick-system (system/mapping-system :tick (fn [m] (infof "I've ticked %d times" m) (inc m))))

(def debug-system
  (system/iterating-system
   :debug
   (fn [entity]
     (clojure.pprint/pprint (ecs/get-all-components entity)))))

(defn add-systems
  [system]
  (-> system
      (ecs/add-system tick-system)
      (ecs/add-system miner-system)
      (ecs/add-system debug-system)))

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

(defn start-loop
  []
  (async/thread (tick-loop 0.5)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (dosync
   (alter sys add-tick-entity)
   (alter sys add-miner-entity)
   (alter sys add-systems))

  (async/<!! (start-loop)))
