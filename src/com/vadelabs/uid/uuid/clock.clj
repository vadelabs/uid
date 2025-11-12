(ns com.vadelabs.uid.uuid.clock
  "Lock-Free, Thread-safe Monotonic Clocks"
  (:require
    [com.vadelabs.uid.uuid.random :as random]))


(deftype State
  [^long seqid ^long millis])


(def ^:const +subcounter-resolution+     9999)


;; =============================================================================
;; Epoch and Time Conversion Constants
;; =============================================================================

(def ^:const gregorian-epoch-offset-100ns
  "Offset from Gregorian epoch (1582-10-15) in 100-nanosecond intervals.
   This constant is used to shift time values to the Gregorian calendar base
   used in UUID v1 and v6 timestamps."
  100103040000000000)

(def ^:const unix-to-universal-time-offset-ms
  "Milliseconds between Unix epoch (1970-01-01) and Universal Time epoch (1900-01-01).

   Calculated as: 70 years × 365.25 days/year × 24 hours/day × 3600 seconds/hour × 1000 ms/second
   Value: 2,208,988,800,000 ms"
  2208988800000)

(def ^:const ms-to-100ns-intervals
  "Conversion factor from milliseconds to 100-nanosecond intervals.

   UUID v1/v6 timestamps use 100-nanosecond intervals as the base unit.
   1 millisecond = 10,000 × 100-nanosecond intervals"
  10000)

(def ^:const gregorian-to-unix-epoch-offset
  "Offset for converting from Gregorian epoch timestamps to Unix epoch timestamps.
   Used in posix-time calculations to shift time base from 1582 to 1970."
  12219292800000)

(def ^:const unix-to-universal-time-offset-seconds
  "Seconds between Unix epoch (1970-01-01) and Universal Time epoch (1900-01-01).

   Calculated as: 70 years × 365.25 days/year × 24 hours/day × 3600 seconds/hour
   Value: 2,208,988,800 seconds"
  2208988800)


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
      (+ (.seqid new-state) gregorian-epoch-offset-100ns
         (* (+ unix-to-universal-time-offset-ms (.millis new-state))
            ms-to-100ns-intervals)))))


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
   (- (quot gregorian ms-to-100ns-intervals) gregorian-to-unix-epoch-offset)))


(defn universal-time
  "Generate the (Common-Lisp compatible) universal-time -- the number of
  seconds that have elapsed since 00:00 January 1, 1900 GMT"
  ([]
   (universal-time (monotonic-time)))
  ([^long gregorian]
   (+ (posix-time gregorian) unix-to-universal-time-offset-seconds)))
