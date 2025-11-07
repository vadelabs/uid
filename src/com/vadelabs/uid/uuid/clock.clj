(ns com.vadelabs.uid.uuid.clock
  "Lock-Free, Thread-safe Monotonic Clocks"
  (:require
    [com.vadelabs.uid.uuid.random :as random]))


(deftype State
  [^long seqid ^long millis])


(def ^:const +subcounter-resolution+     9999)


(let [-state- (atom (->State 0 0))]
  (defn monotonic-time
    "Generate a guaranteed monotonically increasing timestamp based on
     Gregorian time and a stateful subcounter"
    []
    (let [^State new-state
          (swap! -state-
                 (fn [^State current-state]
                   (loop []
                     (let [time-now (System/currentTimeMillis)]
                       (cond
                         (< (.millis current-state) time-now)
                         (->State 0 time-now)

                         (> (.millis current-state) time-now)
                         (recur)

                         :else
                         (let [tt (inc (.seqid current-state))]
                           (if (<= tt +subcounter-resolution+)
                             (->State tt time-now)
                             (recur))))))))]
      (+ (.seqid new-state) 100103040000000000
         (* (+ 2208988800000 (.millis new-state)) 10000)))))


(def ^:const +random-counter-resolution+ 0xfff)


(let [-state- (atom (->State 0 0))]
  (defn monotonic-unix-time-and-random-counter
    "Generate guaranteed monotonically increasing number pairs based on
     POSIX time and a randomly seeded subcounter"
    []
    (let [^State new-state
          (swap! -state-
                 (fn [^State current-state]
                   (loop []
                     (let [time-now (System/currentTimeMillis)]
                       (cond
                         (< (.millis current-state) time-now)
                         (->State (random/eight-bits) time-now)

                         (> (.millis current-state) time-now)
                         (recur)

                         :else
                         (let [tt (inc (.seqid current-state))]
                           (if (<= tt +random-counter-resolution+)
                             (->State tt time-now)
                             (recur))))))))]
      [(.millis new-state) (.seqid new-state)])))


(defn posix-time
  "Generate the (Unix compatible) POSIX time -- the number of seconds
  that have elaspsed since 00:00 January 1, 1970 UTC"
  ([]
   (posix-time (System/currentTimeMillis)))
  ([^long gregorian]
   (- (quot gregorian 10000) 12219292800000)))


(defn universal-time
  "Generate the (Common-Lisp compatible) universal-time -- the number of
  seconds that have elapsed since 00:00 January 1, 1900 GMT"
  ([]
   (universal-time (monotonic-time)))
  ([^long gregorian]
   (+ (posix-time gregorian) 2208988800)))
