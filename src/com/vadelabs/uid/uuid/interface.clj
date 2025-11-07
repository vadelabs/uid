(ns com.vadelabs.uid.uuid.interface
  "UUID generation and manipulation utilities following RFC9562.

   Supports generation of:
   - v0: Null UUID
   - v1: Time-based UUID
   - v3: Name-based UUID (MD5)
   - v4: Random UUID
   - v5: Name-based UUID (SHA1)
   - v6: Time-based, lexically sortable UUID
   - v7: Unix time-based, lexically sortable UUID
   - v8: Custom UUID
   - squuid: Sequential UUID (non-standard)"
  (:refer-clojure :exclude [uuid? max < > =])
  (:require
    [com.vadelabs.uid.uuid.core :as core])
  (:import
    (java.util
      UUID)))


;; Constants
(def +null+ core/+null+)
(def +max+ core/+max+)
(def +namespace-dns+ core/+namespace-dns+)
(def +namespace-url+ core/+namespace-url+)
(def +namespace-oid+ core/+namespace-oid+)
(def +namespace-x500+ core/+namespace-x500+)


;; UUID Generation
(defn null
  "Generates the v0 (null) UUID: 00000000-0000-0000-0000-000000000000"
  ^UUID []
  (core/null))


(defn v0
  "Generates the v0 (null) UUID: 00000000-0000-0000-0000-000000000000"
  ^UUID []
  (core/v0))


(defn max
  "Generates the v15 (maximum) UUID: ffffffff-ffff-ffff-ffff-ffffffffffff"
  ^UUID []
  (core/max))


(defn v1
  "Generate a v1 (time-based) unique identifier, guaranteed to be unique
  and thread-safe regardless of clock precision or degree of concurrency."
  ^UUID []
  (core/v1))


(defn v3
  "Generate a v3 (name based, MD5 hash) UUID.

   Args:
   - context: A UUIDable context (namespace)
   - local-part: The name to hash within the namespace"
  ^UUID [context local-part]
  (core/v3 context local-part))


(defn v4
  "Generate a v4 (random) UUID.

   With no arguments, uses default JVM implementation.
   With two long arguments (msb, lsb), constructs a valid v4 UUID from those values."
  (^UUID []
   (core/v4))
  (^UUID [msb lsb]
   (core/v4 msb lsb)))


(defn v5
  "Generate a v5 (name based, SHA1 hash) UUID.

   Args:
   - context: A UUIDable context (namespace)
   - local-part: The name to hash within the namespace"
  ^UUID [context local-part]
  (core/v5 context local-part))


(defn v6
  "Generate a v6 (time-based), lexically sortable, unique identifier.
   v6 is a field-compatible version of v1, reordered for improved DB locality."
  ^UUID []
  (core/v6))


(defn v7
  "Generate a v7 unix time-based, lexically sortable UUID with monotonic
  counter and cryptographically secure random portion."
  ^UUID []
  (core/v7))


(defn v8
  "Generate a v8 custom UUID with up to 122 bits of user data.

   Args:
   - msb: Most significant 64 bits
   - lsb: Least significant 64 bits"
  ^UUID [^long msb ^long lsb]
  (core/v8 msb lsb))


(defn squuid
  "Generate a SQUUID (sequential, random) unique identifier.
   Non-standard variation on v4 that increases sequentially over time."
  ^UUID []
  (core/squuid))


;; Time utilities
(defn monotonic-time
  "Return a monotonic timestamp based on Gregorian calendar."
  []
  (core/monotonic-time))


;; Protocols (exposed for extension)
(def UUIDNameBytes core/UUIDNameBytes)
(def UUIDable core/UUIDable)
(def UUIDRfc9562 core/UUIDRfc9562)
(def UUIDRfc4122 core/UUIDRfc4122)


;; Protocol functions
(defn as-byte-array
  "Extract a byte serialization that represents the 'name' of x."
  [x]
  (core/as-byte-array x))


(defn as-uuid
  "Coerce the value 'x' to a UUID."
  ^UUID [x]
  (core/as-uuid x))


(defn uuidable?
  "Return true if 'x' can be coerced to UUID."
  [x]
  (core/uuidable? x))


(defn uuid?
  "Return true if x is a UUID."
  [x]
  (core/uuid? x))


(defn null?
  "Return true if UUID has all 128 bits set to zero."
  [uuid]
  (core/null? uuid))


(defn max?
  "Return true if UUID has all 128 bits set."
  [uuid]
  (core/max? uuid))


(defn uuid=
  "Compare two UUIDs for equality."
  [x y]
  (core/uuid= x y))


(defn uuid<
  "Compare two UUIDs for < relation."
  [x y]
  (core/uuid< x y))


(defn uuid>
  "Compare two UUIDs for > relation."
  [x y]
  (core/uuid> x y))


(defn =
  "Compare two or more UUIDs for equality."
  ([_] true)
  ([x y] (core/= x y))
  ([x y & more] (apply core/= x y more)))


(defn <
  "Compare two or more UUIDs for < relation."
  ([_] true)
  ([x y] (core/< x y))
  ([x y & more] (apply core/< x y more)))


(defn >
  "Compare two or more UUIDs for > relation."
  ([_] true)
  ([x y] (core/> x y))
  ([x y & more] (apply core/> x y more)))


;; UUID accessors
(defn get-version
  "Return the version number associated with this UUID."
  [uuid]
  (core/get-version uuid))


(defn get-variant
  "Return the variant number associated with this UUID."
  [uuid]
  (core/get-variant uuid))


(defn get-word-high
  "Return the high 64 bits (most significant word) of UUID."
  ^long [uuid]
  (core/get-word-high uuid))


(defn get-word-low
  "Return the low 64 bits (least significant word) of UUID."
  ^long [uuid]
  (core/get-word-low uuid))


(defn get-time-low
  "Return the time_low field (32 bits) of UUID."
  ^long [uuid]
  (core/get-time-low uuid))


(defn get-time-mid
  "Return the time_mid field (16 bits) of UUID."
  ^long [uuid]
  (core/get-time-mid uuid))


(defn get-time-high
  "Return the time_high field (12 bits) of UUID."
  ^long [uuid]
  (core/get-time-high uuid))


(defn get-clk-high
  "Return the high 6 bits of the clock sequence."
  ^long [uuid]
  (core/get-clk-high uuid))


(defn get-clk-low
  "Return the low 8 bits of the clock sequence."
  ^long [uuid]
  (core/get-clk-low uuid))


(defn get-timestamp
  "Return the timestamp associated with time-based UUIDs (v1, v6, v7).
   Returns nil for non-time-based UUIDs."
  [uuid]
  (core/get-timestamp uuid))


(defn get-instant
  "Return a java.util.Date object for time-based UUIDs (v1, v6, v7).
   Returns nil for non-time-based UUIDs."
  ^java.util.Date [uuid]
  (core/get-instant uuid))


(defn get-unix-time
  "Return the unix time (milliseconds since epoch) for time-based UUIDs.
   Returns nil for non-time-based UUIDs."
  [uuid]
  (core/get-unix-time uuid))


(defn get-node-id
  "Return the 48-bit node identifier."
  [uuid]
  (core/get-node-id uuid))


(defn get-clk-seq
  "Return the clock-sequence number for v1/v6 UUIDs.
   Returns nil for other versions."
  [uuid]
  (core/get-clk-seq uuid))


;; Conversions
(defn to-byte-array
  "Return an array of 16 bytes that represents the UUID."
  [uuid]
  (core/to-byte-array uuid))


(defn to-string
  "Return a string representation of the UUID in canonical format."
  ^String [uuid]
  (core/to-string uuid))


(defn to-hex-string
  "Return a 32-character hex string (no dashes)."
  ^String [uuid]
  (core/to-hex-string uuid))


(defn to-urn-string
  "Return a URN string representation."
  ^String [uuid]
  (core/to-urn-string uuid))


(defn to-uri
  "Return the unique URN URI associated with this UUID."
  ^java.net.URI [uuid]
  (core/to-uri uuid))


;; Predicates
(defn uuid-string?
  "Return true if str is a valid UUID string."
  [s]
  (core/uuid-string? s))


(defn uuid-urn-string?
  "Return true if str is a valid UUID URN string."
  [s]
  (core/uuid-urn-string? s))
