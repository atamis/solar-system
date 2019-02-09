(ns solar-system.core
  (:require [brute.entity :as e]
            [brute.system :as s]
            [taoensso.timbre :as timbre]
            [solar-system.ecs :as ecs]
            )
  (:gen-class))

(timbre/refer-timbre)

(def ^:dynamic get-component nil)

(def sys (ref (ecs/create-system)))

(defn mapping-system
  [component fun]
  (fn [system delta]
    (reduce
     (fn [system entity]
       (e/add-component
        system
        entity
        (binding [get-component (fn [component] (e/get-component system entity component))] (fun entity delta)))
       )
     system
     (e/get-all-entities-with-component system component))
    )
  )

(defrecord TickComponent [n])

(defn tick-inc
  [^TickComponent t]
  (update-in t [:n] inc)
  )

(defn tick-system
  [system _delta]
  (reduce
   (fn [system entity]
     (-> system
         (e/update-component entity TickComponent tick-inc)
         )
     )
   system
   (e/get-all-entities-with-component system TickComponent))
  )

(def tick-system2
  (mapping-system :tick2
                  (fn [entity delta]
                    (let [component (get-component :tick2)]
                      (infof "I have ticked %d times" (:n component))
                      (update-in component [:n] inc)))))


(defn add-tick-entity
  [system]
  (let [entity (e/create-entity)]
    (-> system
        (e/add-entity entity)
        (e/add-component entity (->TickComponent 0))
        (e/add-component entity {:component :tick2 :n 0})
        )
    )
  )

(defn add-tick-entity2
  [system]
  (let [entity (e/create-entity)]
    (-> system
        (e/add-entity entity)
        (e/add-component entity {:component :tick2 :n 0})
        )
    )
  )



(defn add-systems
  [system]
  (-> system
      (s/add-system-fn tick-system)
      (s/add-system-fn tick-system2)
      )
  )

(defn tick!
  []
  (dosync
   (alter sys #(s/process-one-game-tick % 1))
   )
  )

(defn current-millis [] (java.lang.System/currentTimeMillis))

(defn tick-loop
  [fps]
  (def frame-time (/ 1000 fps))
  (loop []
    (def start (current-millis))
    (tick!)
    (def end (current-millis))
    (def length (- end start))
    (def sleeptime (- frame-time length))
    (infof "Tick loop took %dms, sleeping %dms" (long length) (long sleeptime))
    (Thread/sleep sleeptime)
    (recur)
    )
  )

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (dosync
   (alter sys add-tick-entity)
   (alter sys add-systems)
   )
  (tick-loop 0.5)
  )
