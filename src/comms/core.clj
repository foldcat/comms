(ns comms.core
  (:require
    [clojure.core.async :as async]
    [comms.impl.records :refer [map->Actor
                                map->Signal
                                map->Call-Response
                                map->Supervisor]
     :as records]))


(defn- get-from
  ([pred coll]
   (->> (filter #(= (first %) pred) coll)
        (first)
        (drop 1))))


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
     :supervisor-chan (volatile! nil)
     :state (volatile! nil)
     :failed? (volatile! false)
     :nm (first (get-from :name coll))
     :identifier nil}))


(defn start!
  "starts a module"
  [module]
  (records/start! module))


(defn cast!
  "send signal to module in an async fashion"
  [module signal]
  (records/cast! module signal))


(defn call!
  "send signal to module, get reply from the module"
  ([module signal]
   (records/call! module signal))
  ([module signal timeout-ms]
   (records/call! module signal timeout-ms)))


(defn supervise
  "supervises a module"
  [actor options]
  (let [supervisor-chan (async/chan)]
    (vreset! (.supervisor-chan actor) supervisor-chan)
    (map->Supervisor ; make supervisor
     {:error-channel supervisor-chan
      :actor (volatile! actor)
      :options options})))


(defn reply
  [reply next-state]
  (map->Call-Response
    {:reply reply
     :next-state next-state}))
