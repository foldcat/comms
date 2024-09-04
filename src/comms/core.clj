(ns comms.core
  "main api to allow operations on actors"
  (:require
    [clojure.core.async :as async]
    [comms.impl.records :refer [map->Actor
                                map->Call-Response
                                map->Supervisor]
     :as records]))


(defn- get-from
  ([pred coll]
   (->> (filter #(= (first %) pred) coll)
        (first)
        (drop 1))))


(defn defmodule
  "make an module, percisely
  ```clojure
  (defmodule
    [[:init (fn [] 0)]
     [:name \"test-actor\"]
     [:handle-cast
      (fn [_signal state]
        (inc state))]
     [:handle-call
      (fn [_sig state]
        (let [new-state (inc state)]
          (reply new-state new-state)))]]))
  ```
  which the
  - :init field takes a function that takes no arguments,
    its return will be the starting state of the module
  - :name field takes a string as the name of the actor 
  - :handle-cast field takes a function with 2 arguments,
    the first one shall be the the signal recieved and 
    the second one will be the current state, the return 
    of this function will be the new state after a cast 
    call, but is called asynchronously
  - :handle-call field takes the same type of function 
    as :handle-cast but is called synchronously, and the 
    function must return a reply

  call the (start!) function to start this module
  "
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
  "send signal to module asynchronously"
  [module signal]
  (records/cast! module signal))


(defn call!
  "send signal to module, get reply from the module"
  ([module signal]
   (records/call! module signal))
  ([module signal timeout-ms]
   (records/call! module signal timeout-ms)))


(defn supervise
  "supervises a module, must be (start!)ed for it to work"
  [actor options]
  (let [supervisor-chan (async/chan)]
    (vreset! (.supervisor-chan actor) supervisor-chan)
    (map->Supervisor ; make supervisor
     {:error-channel supervisor-chan
      :actor (volatile! actor)
      :options options})))


(defn reply
  "returned by a :handle-call, first argument is the 
  data to reply and next-state will be the next 
  state for the actor"
  [reply next-state]
  (map->Call-Response
    {:reply reply
     :next-state next-state}))
