(ns comms.impl.records)


(defrecord Actor
  [proc ; thread obj
   started? ; is mod started
   init ; fn runs on start
   handle-call ; sync call handle
   handle-cast  ; async call handle
   mailbox ; mailbox
   supervisor ; supervisor 
   state]) ; state, volatile

(defmethod print-method Actor [v ^java.io.Writer w]
  (.write w "@actor[")
  (.write w (.toString v))
  (.write w "]"))


(defrecord Signal
  [category ; type of the signal
   signal ; content of the signal
   module ; module to send to
   carrier]) ; a promise

(defmethod print-method Signal [v ^java.io.Writer w]
  (.write w "@actor[")
  (.write w (.toString v))
  (.write w "]"))


(defrecord Call-Response
  [mode ; :error / :success 
   reply ; data to reply
   next-state]) ; next state

(defmethod print-method Call-Response [v ^java.io.Writer w]
  (.write w "@actor[")
  (.write w (.toString v))
  (.write w "]"))
