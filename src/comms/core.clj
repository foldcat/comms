(ns comms.core
  (:require
    [clojure.core.async :as async]
    [comms.impl.execution :refer [route-exe]]
    [comms.impl.records :refer [map->Actor
                                map->Signal
                                map->Call-Response]]
    [comms.impl.vthread :refer [vthread]]
    [taoensso.timbre :as log]))


(defn- get-from
  [pred coll]
  (->> (filter #(= (first %) pred) coll)
       (first)
       (drop 1)))


(defn defmodule
  "make an module"
  [coll]
  (map->Actor
    {:proc nil
     :started? false
     :init (get-from :init coll)
     :handle-call (get-from :handle-call coll)
     :handle-cast (get-from :handle-cast coll)
     :mailbox (async/chan 10)
     :supervisor nil
     :state (volatile! nil)}))


(defn- -execution-loop
  [module]
  (let [starting-state ((first (.init module)))]
    (vswap! (.state module) (constantly starting-state)))
  (loop []
    (let [signal (async/<!! (.mailbox module))
          next-state (constantly (route-exe signal))]
      (log/info "-----next state -----")
      (log/info next-state)
      (vswap! (.state module) next-state))
    (recur)))


(defn start!
  "starts a module"
  [module]
  (let
    [proc
     (vthread (-execution-loop module))
     out (assoc module :proc proc)]
    out))


(defn cast!
  "send signal to module in an async fashion"
  [module signal]
  (async/>!! (.mailbox module)
             (map->Signal
               {:category :cast
                :signal signal
                :module module
                :carrier nil}))) ; don't need a carrier here


(defn- -!call!
  "helper function for `call!call!`"
  [module signal]
  (let [carrier (promise)]
    (async/>!! (.mailbox module)
               (map->Signal
                 {:category :call
                  :signal signal
                  :module module
                  :carrier carrier}))
    carrier))


(defn call!
  "send signal to module, get reply from the module"
  ([module signal]
   (deref (-!call! module signal)))
  ([module signal timeout-ms]
   (deref (-!call! module signal) timeout-ms :nil))) ; TODO: change


(defn supervise!
  [actors])


(defn success
  [reply next-state]
  (map->Call-Response
    {:mode :success
     :reply reply
     :next-state next-state}))


(defn fail
  [fail]
  (map->Call-Response
    {:mode :fail
     :reply nil
     :next-state nil}))


(comment
  (letfn [(hcast
            [sig state]
            (case sig
              :inc (do
                     (log/info "inc")
                     (inc state))
              :dec (do
                     (log/info "dec")
                     (dec state))))
          (hcall 
            [sig state]
            (case sig 
              :inc (let [new-state (inc state)]
                     (log/info "inc call")
                     (success new-state new-state))))]

    (let [module (defmodule
                   [[:init (fn [] 0)]
                    [:handle-cast hcast]
                    [:handle-call hcall]])]
      (start! module)
      (cast! module :inc)
      (log/info "-----call-----")
      (log/info (call! module :inc))
      (log/info "-----end call-----")
      (Thread/sleep 1000)
      @(.state module))))
