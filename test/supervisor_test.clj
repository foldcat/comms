(ns supervisor-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [comms.core :as core]
    [comms.impl.records :as rec]))


(def await-ms 500)


(deftest ^:integration supervisor-test
  (testing "can supervisor restart actor"
    (is (= 5 ; first time is when the mod first starts
           (let [num-started (atom 0)
                 supervisor
                 (core/supervise
                   (core/defmodule
                     [[:init (fn [] (swap! num-started inc))]
                      [:name "test-actor"]
                      [:handle-cast
                       (fn [_signal _state]
                         (throw (RuntimeException. "test error")))]])
                   :restart)]
             (core/start! supervisor)
             (Thread/sleep await-ms)
             (dotimes [_ 4]
               (rec/cast! supervisor :whatever)
               (Thread/sleep await-ms))
             @num-started)))))
