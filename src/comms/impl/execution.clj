(ns comms.impl.execution
  (:require
    [comms.impl.logging :as log]))


(defn router
  [signal]
  (:category signal))


(defmulti route-exe #'router)


(defmethod route-exe :cast
  [signal]
  (log/debug "-----route cast exe-----")
  ;; (log/debug signal)
  (let [sig (.signal signal)
        module (.module signal)
        state (.state module)
        cast-fn (first (.handle-cast module))
        next-state (cast-fn sig @state)]
    next-state))


(defmethod route-exe :call
  [signal]
  (log/debug "-----route call exe-----")
  (log/debug signal)
  (let [module (.module signal)
        sig (.signal signal)
        state (.state module)
        carrier (.carrier signal)
        cast-fn (first (.handle-call module))
        next-state (cast-fn sig @state)]
    (log/debug "-----next state-----")
    (log/debug next-state)
    (deliver carrier (.reply next-state))
    (.next-state next-state)))


(defmethod route-exe :default
  [signal]
  (log/debug signal)
  (log/debug (.category signal)))
