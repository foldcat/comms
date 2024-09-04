(ns actor-start-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [comms.core :as core]))


(def await-ms 500)


(def actor
  (core/defmodule
    [[:init (fn [] 0)]
     [:name "test-actor"]
     [:handle-cast
      (fn [_signal state]
        (inc state))]
     [:handle-call
      (fn [_sig state]
        (let [new-state (inc state)]
          (core/reply new-state new-state)))]]))


(deftest ^:integration actor-test
  (testing "init actor state"
    (is (= 0
           (let [s (:state (core/start! actor))]
             (Thread/sleep await-ms)
             (deref s)))))

  (testing "handle cast"
    (is (= 1
           (let [s (core/start! actor)]
             (core/cast! s :inc)
             (Thread/sleep await-ms)
             (deref (:state s))))))

  (testing "handle call"
    (is (= 1
           (let [s (core/start! actor)]
             (core/call! s :inc))))))
