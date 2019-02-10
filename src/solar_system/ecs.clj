(ns solar-system.ecs
  (:require [brute.entity :as entity]
            [brute.system :as system]))

(defprotocol Ireference? (reference? [this]))

(extend-type java.lang.Object Ireference? (reference? [this] false))
(extend-type nil Ireference? (reference? [this] false))
(extend-type clojure.lang.Ref Ireference? (reference? [this] true))
(extend-type clojure.lang.Agent Ireference? (reference? [this] true))

(defmethod entity/get-component-type clojure.lang.PersistentArrayMap
  [component]
  (:component component))

(defmethod entity/get-component-type clojure.lang.PersistentHashMap
  [component]
  (:component component))

(def get-component-type entity/get-component-type)

(defn add-event-internal
  [system event]
  (swap! (:events (if (reference? system) @system system)) conj event))

(defn drain-events
  [system]
  (let [[old-events _] (reset-vals! (:events (if (reference? system) @system system)) [])]
    old-events))

(defn create-system
  []
  (assoc (entity/create-system) :events (atom [])))

(defn ^:private remove-component-internal
  "Remove a component instance from the ES data structure and returns it"
  [system entity type]
  (let [system (transient system)
        entity-components (:entity-components system)
        entity-component-types (:entity-component-types system)]
    (-> system
        (assoc! :entity-components (assoc entity-components type (-> entity-components (get type) (dissoc entity))))
        (assoc! :entity-component-types (assoc entity-component-types entity (-> entity-component-types (get entity) (disj type))))
        persistent!)))

(declare iterating-system)

(def ^:dynamic get-component entity/get-component)
(def ^:dynamic add-entity entity/add-entity)
(def ^:dynamic add-component entity/add-component)
(def ^:dynamic kill-entity entity/kill-entity)
(def ^:dynamic remove-component remove-component-internal)
(def ^:dynamic add-event add-event-internal)
(def ^:dynamic add-entity! nil)
(def ^:dynamic current-sys nil)
(def create-entity entity/create-entity)
(def add-system system/add-system-fn)
(def get-all-entities-with-component entity/get-all-entities-with-component)
(defn add-iterating-system
  [system type fun]
  (add-system system (iterating-system type fun)))
(def process-tick system/process-one-game-tick)

(defn ^:private localize-get-component
  [sys-ref entity]
  (fn [component]
    (entity/get-component @sys-ref entity component)))

(defn ^:private localize-add-entity
  [sys-ref]
  (fn
    ([] (dosync (alter sys-ref #(entity/add-entity %1 (entity/create-entity)))))
    ([entity] (dosync (alter sys-ref #(entity/add-entity %1 entity))))))

(defn ^:private localize-add-component
  [sys-ref local-entity]
  (fn
    ([component] (dosync (alter sys-ref #(entity/add-component %1 local-entity component))))
    ([entity component] (dosync (alter sys-ref #(entity/add-component %1 entity component))))))

(defn ^:private localize-kill-entity
  [sys-ref local-entity]
  (fn
    ([] (dosync (alter sys-ref #(entity/kill-entity %1 local-entity))))
    ([entity] (alter sys-ref #(entity/kill-entity %1 entity)))))

(defn ^:private localize-remove-component
  [sys-ref local-entity local-type]
  (fn
    ([] (dosync (alter sys-ref #(remove-component-internal %1 local-entity local-type))))
    ([type] (dosync  (alter sys-ref #(remove-component-internal %1 local-entity type))))
    ([entity type] (dosync (alter sys-ref #(remove-component-internal %1 entity type))))))

(defn ^:private localize-add-event
  [sys-ref]
  (fn
    ([event] (add-event-internal sys-ref event))
    ([system event] (add-event-internal system event))))

(defn ^:private localize-add-entity!
  [sys-ref]
  (fn
    [] (let [entity (entity/create-entity)]
         (dosync (alter sys-ref #(entity/add-entity %1 entity)))
         entity)))

(defn iterating-system
  [type fun]
  (fn [system delta]
    (let [sys-ref (ref system)]
      (doseq [entity (entity/get-all-entities-with-component system type)]
        (let [local-entity entity
              local-type   type]
          (binding [get-component    (localize-get-component sys-ref local-entity)
                    add-entity       (localize-add-entity sys-ref)
                    add-component    (localize-add-component sys-ref local-entity)
                    kill-entity      (localize-kill-entity sys-ref local-entity)
                    remove-component (localize-remove-component sys-ref local-entity local-type)
                    add-event        (localize-add-event sys-ref)
                    add-entity!      (localize-add-entity! sys-ref)
                    current-sys      (fn [] @sys-ref)]
            (fun entity))))
      @sys-ref)))

(defn add-singleton-ref
  ([system fun] (add-singleton-ref system {} fun))
  ([system data fun] (add-singleton-ref system (gensym) data fun))
  ([system type data fun]
   (let [entity    (create-entity)
         component (assoc data :component type)
         system    (-> system
                       (add-entity entity)
                       (add-component entity component)
                       (add-iterating-system type fun))]
     [system entity])))

(defn add-singleton
  ([system fun] (add-singleton system {} fun))
  ([system data fun] (add-singleton system (gensym) data fun))
  ([system type data fun]
   (let [[system entity] (add-singleton-ref system type data fun)]
     system)))
