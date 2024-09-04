(ns comms.impl.flakes
  (:require
    [flake.core :as flake]))


(defonce initialized? (volatile! false))


(defn- new-flake
  []
  (->> (flake/generate!)
       (flake/flake->bigint)))


(defn genflake
  []
  (if @initialized?
    (new-flake)
    (do (flake/init!)
        (vswap! initialized? not)
        (new-flake))))
