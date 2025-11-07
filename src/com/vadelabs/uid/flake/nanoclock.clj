(ns com.vadelabs.uid.flake.nanoclock
  "High-precision timestamp provider combining wall clock and monotonic timing.

  This implementation anchors to the system wall clock at initialization,
  then uses monotonic timing to provide nanosecond-precision timestamps
  that maintain consistent intervals.

  Key characteristics:
  - Nanosecond resolution for timestamp generation
  - Monotonic properties within process lifetime
  - Wall-clock anchored for cross-system compatibility
  - Single global instance for consistency

  Note: This is not a high-precision wall clock replacement. While it provides
  nanosecond granularity, it can drift from true wall time over extended periods
  as monotonic clocks don't adjust for NTP synchronization.

  Timestamp range extends to approximately year 2262 when using nanosecond
  precision in a long value.")


(defn- now-value
  "Returns the current time as milliseconds since Unix epoch."
  []
  (System/currentTimeMillis))


(defn- nano
  "Returns the current value of the running JVM's high-resolution time source, in nanoseconds."
  []
  (System/nanoTime))


(defonce ^:private clock
  (let [tw0 (* (now-value) 1000000)
        tm0 (nano)]
    {:wall-start tw0
     :mono-start tm0}))

(defn current-time-nanos
  []
  (+ (:wall-start clock)
     (- (nano) (:mono-start clock))))

(defn current-time-micros
  []
  (quot (current-time-nanos) 1000))

(defn current-time-millis
  []
  (quot (current-time-nanos) 1000000))