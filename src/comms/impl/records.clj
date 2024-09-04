(ns comms.impl.records
  "various records for performance reason"
  (:require
    [clojure.core.async :as async]
    [comms.impl.execution :refer [-actor-execution-loop]]
    [comms.impl.logging :as log]
    [comms.impl.vthread :refer [vthread]]))


(defrecord Signal
  [category ; type of the signal
   signal ; content of the signal
   module ; module to send to
   carrier]) ; a promise

(defmethod print-method Signal [v ^java.io.Writer w]
  (.write w "@actor[")
  (.write w (.toString v))
  (.write w "]"))


(defprotocol Startable
  "can a record be started"

  (start!
    [this]
    "start this")

  (call!
    [this signal]
    [this signal timeout-ms])

  (cast! [this signal]))


(defn -!call!
  "helper function for `call!`"
  [module signal]
  (let [carrier (promise)]
    (async/>!! (.mailbox module)
               (map->Signal
                 {:category :call
                  :signal signal
                  :module module
                  :carrier carrier}))
    carrier))


(defrecord Actor
  [proc ; thread obj
   nm ; name of the actor
   started? ; is mod started
   init ; fn runs on start
   handle-call ; sync call handle
   handle-cast  ; async call handle
   mailbox ; mailbox
   supervisor-chan ; supervisor channel
   state ; state, volatile
   failed? ; is said actor failed
   identifier] ; flake id gaven by supervisor

  Startable

  (start!
    [this]
    (log/debug this)
    (let
      [proc
       (vthread (-actor-execution-loop this))
       out (-> (assoc this :proc proc)
               (assoc :started? true))]
      out))


  (call!
    [this signal]
    (let [prom (-!call! this signal)]
      @prom))


  (call!
    [this signal timeout-ms]
    (let [prom (-!call! this signal)]
      (deref prom timeout-ms :todo))) ; TODO

  (cast!
    [this signal]
    (async/>!! (.mailbox this)
               (map->Signal
                 {:category :cast
                  :signal signal
                  :module this}))))


(defmethod print-method Actor [v ^java.io.Writer w]
  (.write w "@actor[")
  (.write w (.toString v))
  (.write w "]"))


(defrecord Call-Response
  [reply ; data to reply
   next-state]) ; next state

(defmethod print-method Call-Response [v ^java.io.Writer w]
  (.write w "@actor[")
  (.write w (.toString v))
  (.write w "]"))


(defn- -supervisor-execution-loop
  "moved here due to cyclic dependency issue"
  [supervisor]
  (loop [times-run 0]
    (let [_err (async/<!! (.error-channel supervisor))
          module @(.actor supervisor)
          restarted-mod (start! module)]
      (log/info "supervisor saw error, restarting" (.nm module))
      (vswap! (.actor supervisor) (constantly restarted-mod))
      (log/info "supervisor run" times-run "times"))
    (recur (inc times-run))))


(defrecord Supervisor
  [error-channel ; channel for listening to error
   actor ; the actor
   options ; error handling fn
   proc ; thread obj
   started?] ; is it started

  Startable

  (start!
    [this]
    (log/debug this)
    (let
      [proc
       (vthread
         (doall (start! @(.actor this)))
         (-supervisor-execution-loop this))
       out (-> (assoc this :proc proc)
               (assoc :started? true))]
      out))


  (call!
    [this signal]
    (call! @(.actor this) signal))


  (call!
    [this signal timeout-ms]
    (call! @(.actor this) signal timeout-ms))


  (cast!
    [this signal]
    (cast! @(.actor this) signal)))


(defmethod print-method Supervisor [v ^java.io.Writer w]
  (.write w "@actor[")
  (.write w (.toString v))
  (.write w "]"))
