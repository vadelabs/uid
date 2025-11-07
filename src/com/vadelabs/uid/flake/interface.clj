(ns com.vadelabs.uid.flake.interface
  "High-performance time-ordered unique identifiers.

   Flakes are 192-bit identifiers designed for distributed systems that need:
   - Guaranteed ordering based on creation time
   - High entropy for uniqueness
   - Compact string representation
   - Fast generation (sub-50ns target)

   ## Structure
   - 64 bits: High-precision timestamp (nanosecond resolution)
   - 128 bits: Cryptographically random data

   ## Key Properties
   - **Monotonic**: Later-generated flakes sort after earlier ones
   - **Order-preserving**: String representations maintain sort order
   - **URL-safe**: Uses web-safe Base64-variant encoding
   - **Collision-resistant**: 128 bits of entropy per timestamp

   ## Performance
   Optimized for high-throughput scenarios with thread-local randomization
   and efficient encoding algorithms."
  (:require
    [com.vadelabs.uid.flake.core :as core]))


(defn flake
  "Generates a new time-ordered unique identifier.

  Returns a 192-bit identifier that is guaranteed to be larger than any
  previously generated Flake in the same thread."
  []
  (core/flake))


(defn snowflake
  "Generates a new Flake as a URL-safe string.

  Returns a 32-character string that preserves the monotonic ordering
  of the underlying Flake values."
  []
  (core/flake-string))


(defn flake-time
  "Extracts the timestamp component in nanoseconds.

  Returns the nanosecond timestamp indicating when the Flake was created."
  [f]
  (core/timestamp f))


(defn flake-hex
  "Returns hexadecimal representation of a Flake.

  Returns a 48-character lowercase hexadecimal string."
  [f]
  (core/as-hex f))


(defn flake?
  "Checks if the given value is a Flake instance."
  [x]
  (core/flake? x))


(defn read-method
  "Reader method for #flake/flake tagged literals."
  [flake-str]
  (core/read-flake flake-str))


(defn from-string
  "Parses a Flake from its string representation.

  Returns a Flake instance or nil if the input is invalid."
  [s]
  (core/from-string s))


(defn make-flake
  "Create a flake with specific timestamp and random components (for testing)"
  ([timestamp-nanos random-high random-low]
   (core/make-flake timestamp-nanos random-high random-low))
  ([byte-data]
   (core/make-flake byte-data)))


(defn flake-bytes
  "Get the byte array representation of a flake"
  [f]
  (core/flake-bytes f))
