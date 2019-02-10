(ns solar-system.util-test
  (:require  [clojure.test :as t]
             [solar-system.util :as util]))


(t/deftest util
  (t/testing "arity-first"
    (t/is 0 (util/arity-min (fn [] nil)))
    (t/is 1 (util/arity-min (fn [x] nil)))
    (t/is 2 (util/arity-min (fn [x y] nil)))
    (t/is 1 (util/arity-min (fn ([x] nil) ([x y] nil))))
    (t/is 1 (util/arity-min (fn ([x y] nil) ([x] nil))))
    )

  (t/testing "arity-all"
    (t/is [0] (util/arity-all (fn [] nil)))
    (t/is [1] (util/arity-all (fn [x] nil)))
    (t/is [2] (util/arity-all (fn [x y] nil)))
    (t/is [1 2] (util/arity-all (fn ([x] nil) ([x y] nil))))
    (t/is [1 2] (util/arity-all (fn ([x y] nil) ([x] nil))))
    (t/is [3 4] (util/arity-all (fn ([x y z] nil) ([x y z w] nil))))
    )

  (t/testing "arity-max"
    (t/is 0 (util/arity-max (fn [] nil)))
    (t/is 1 (util/arity-max (fn [x] nil)))
    (t/is 2 (util/arity-max (fn [x y] nil)))
    (t/is 2 (util/arity-max (fn ([x] nil) ([x y] nil))))
    (t/is 2 (util/arity-max (fn ([x y] nil) ([x] nil))))
    (t/is 4 (util/arity-max (fn ([x y z] nil) ([x y z w] nil))))
    )
  )
