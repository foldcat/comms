(ns comms.impl.logging
  (:require
    [clojure.string :as cs])
  (:import
    org.slf4j.LoggerFactory))


(defonce logger (LoggerFactory/getLogger "comms"))


(defn error
  [& s]
  (.error logger (cs/join " " s)))


(defn warn
  [& s]
  (.warn logger (cs/join " " s)))


(defn info
  [& s]
  (.info logger (cs/join " " s)))


(defn debug
  [& s]
  (.debug logger (cs/join " " s)))


(defn trace
  [& s]
  (.trace logger (cs/join " " s)))
