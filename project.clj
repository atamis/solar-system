(defproject solar-system "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [brute "0.4.0"]
                 [betrayer "0.1.0-SNAPSHOT"]
                 [com.taoensso/timbre "4.10.0"]
                 [org.clojure/core.async "0.4.490"]
                 [com.rpl/specter "1.1.2"]
                 ]
  :main ^:skip-aot solar-system.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
