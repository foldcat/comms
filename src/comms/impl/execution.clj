(ns comms.impl.execution
  (:require
    [taoensso.timbre :as log]))


(defn router
  [signal]
  (:category signal))


(defmulti route-exe #'router)


(defmethod route-exe :cast
  [signal]
  (log/info "-----route cast exe-----")
  ;; (log/info signal)
  (let [sig (.signal signal)
        module (.module signal)
        state (.state module)
        cast-fn (first (.handle-cast module))
        next-state (cast-fn sig @state)]
    next-state))


(defmethod route-exe :call
  [signal]
  (log/info "-----route call exe-----")
  (log/info signal)
  (let [module (.module signal)
        sig (.signal signal)
        state (.state module)
        carrier (.carrier signal)
        cast-fn (first (.handle-call module))
        next-state (cast-fn sig @state)]
    (log/info "-----next state-----")
    (log/info next-state)
    (if (= :fail (.mode next-state))
      (throw (RuntimeException. "something is wrong")) ; fail
      (do (deliver carrier (.reply next-state))
          (.next-state next-state)))))


(defmethod route-exe :default
  [signal]
  (log/info signal)
  (log/info (.category signal)))
