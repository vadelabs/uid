(ns com.vadelabs.uid.flake.core
  "Core implementation for high-performance unique identifiers.

   Provides functions for generating and manipulating Flakes - 192-bit identifiers
   that combine nanosecond timestamps with high-entropy random data to ensure
   both uniqueness and monotonic ordering properties."
  (:require
    [com.vadelabs.uid.flake.impl :as impl]
    [com.vadelabs.uid.flake.nanoclock :as clock])
  (:import
    (com.vadelabs.uid.flake.impl
      Flake)))


(defn flake
  "Generates a new Flake with current timestamp and random components.

  Returns a 192-bit identifier that is guaranteed to be larger than any
  previously generated Flake in the same thread."
  []
  (impl/flake))


(defn flake-string
  "Generates a new Flake as a URL-safe string representation.

  Returns a 32-character string that preserves the monotonic ordering
  of the underlying Flake values."
  []
  (impl/flake->string (impl/flake)))


(defn timestamp
  "Extracts the timestamp component from a Flake.

  Returns the nanosecond timestamp indicating when the Flake was created."
  [f]
  (impl/timestamp-nanos f))


(defn as-hex
  "Converts a Flake to its hexadecimal representation.

  Returns a 48-character lowercase hexadecimal string."
  [f]
  (impl/flake->hex f))


(defn flake?
  "Checks if the given value is a Flake instance."
  [x]
  (impl/flake? x))


(defn from-string
  "Parses a Flake from its string representation.

  Returns a Flake instance or nil if the input is invalid."
  [s]
  (impl/parse-flake s))


(defn make-flake
  "Create a flake with specific timestamp and random components (for testing)"
  ([timestamp-nanos random-high random-low]
   (impl/make-flake timestamp-nanos random-high random-low))
  ([byte-data]
   (impl/make-flake byte-data)))


(defn compare-flakes
  "Compare two flakes, returns negative if f1 < f2, zero if equal, positive if f1 > f2"
  [f1 f2]
  (compare f1 f2))


(defmethod print-method Flake
  [f ^java.io.Writer w]
  (.write w "#flake/id ")
  (print-method (impl/flake->string f) w))


(defmethod print-dup Flake
  [f ^java.io.Writer w]
  (print-method f w))


(defn as-bytes
  "Returns the byte array representation of a Flake.

  Returns a 24-byte array containing the Flake's raw data."
  [f]
  (impl/flake->bytes f))


(defn timestamp-millis
  "Extracts the timestamp component as milliseconds since epoch.

  Converts the nanosecond timestamp to milliseconds for easier
  interoperability with standard time libraries."
  [f]
  (quot (impl/timestamp-nanos f) impl/nanos-per-milli))


(defn age-nanos
  "Returns the age of a Flake in nanoseconds.

  Calculates the time elapsed since the Flake was created."
  [f]
  (- (clock/current-time-nanos)
     (impl/timestamp-nanos f)))


(defn age-millis
  "Returns the age of a Flake in milliseconds.

  Calculates the time elapsed since the Flake was created."
  [f]
  (quot (age-nanos f) impl/nanos-per-milli))


(defn read-flake
  "Reader method for #flake/flake tagged literals."
  [flake-str]
  {:pre [(string? flake-str)]}
  (from-string flake-str))
