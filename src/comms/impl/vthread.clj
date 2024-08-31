(ns comms.impl.vthread
  (:require
    [clojure.core.async :as a]))


(defmacro vthread
  "runs expr in a virtual thread immediately"
  [& expr]
  `(let [t# (.start (Thread/ofVirtual)
                    (fn [] ~@expr))]
     (.setName t# (str (gensym "comms-actor-")))
     t#))


(comment 
  (let [logging-chan (a/chan 20)]
    (vthread
      (loop []
        (locking *out*
          (println (a/<!! logging-chan)))
        (recur)))

    (let [c (atom [])
          target 1000]
      (doseq [a (range target)]
        (vthread
          (Thread/sleep 2000)
          (swap! c #(conj % a))))
      (vthread
        (a/>!! logging-chan
               (with-out-str
                 (time (loop []
                         (if (= (count @c) target)
                           (locking *out* 
                             (println "finished"))
                           (recur))))))))))
