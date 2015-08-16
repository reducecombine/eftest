(ns eftest.core
  (:require [clojure.java.io :as io]
            [clojure.test :as test]
            [clojure.tools.namespace.find :as find]))

(defmethod test/report :begin-test-run [_])

(defn require-namespaces-in-dir [dir]
  (map (fn [ns] (require ns) (find-ns ns)) (find/find-namespaces-in-dir (io/file dir))))

(defn find-tests-in-namespace [ns]
  (->> ns ns-interns vals (filter (comp :test meta))))

(defn find-tests-in-dir [dir]
  (mapcat find-tests-in-namespace (require-namespaces-in-dir dir)))

(defn synchronize [f]
  (let [lock (Object.)]
    (fn [x] (locking lock (f x)))))

(defn test-vars [vars]
  (doseq [[ns vars] (group-by (comp :ns meta) vars)]
    (let [once-fixtures (-> ns meta ::test/once-fixtures test/join-fixtures)
          each-fixtures (-> ns meta ::test/each-fixtures test/join-fixtures)
          report        (synchronize test/report)
          test-var      (fn [v] (binding [test/report report] (test/test-var v)))]
      (once-fixtures
       (fn [] (dorun (pmap (bound-fn [v] (each-fixtures #(test-var v))) vars)))))))

(defn test-dir [dir]
  (test-vars (find-tests-in-dir dir)))

(defn run-tests [dir]
  (test/do-report {:type :begin-test-run})
  (test-dir dir))
