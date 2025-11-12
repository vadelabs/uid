(ns com.vadelabs.uid.interface
  "Unified interface for unique identifier generation.

   This namespace provides a curated, client-friendly API exposing the most
   commonly used UUID and Flake operations. For advanced features and low-level
   operations, use the core namespaces directly:
   - `com.vadelabs.uid.uuid.core` for UUID internals
   - `com.vadelabs.uid.flake.core` for Flake internals

   ## Quick Start - UUID

   ```clojure
   (require '[com.vadelabs.uid.interface :as uid])

   ;; Random UUID (most common)
   (uid/v4)  ;=> #uuid \"550e8400-e29b-41d4-a716-446655440000\"

   ;; Time-ordered, sortable UUID (recommended for databases)
   (uid/v7)  ;=> #uuid \"01890a5d-ac96-774b-bcce-b302099a8057\"

   ;; Name-based UUID (deterministic)
   (uid/v5 uid/+namespace-dns+ \"example.com\")
   ```

   ## Quick Start - Flake

   ```clojure
   ;; Generate Flake (192-bit, nanosecond precision)
   (uid/flake)  ;=> #flake/id \"56S2f9LfyJJ52sY7JJuWL-etkrr2OBOC\"

   ;; Generate as string directly
   (uid/snowflake)  ;=> \"56S2f9Lg0JJ52sY7JJuWL-etkrr2OBOC\"
   ```

   ## Design Philosophy

   This interface exposes:
   - ✓ High-frequency operations (generation, parsing, comparison)
   - ✓ Essential metadata extraction (timestamps from time-based IDs)
   - ✗ Low-level bit manipulation (use core namespaces)
   - ✗ Rarely-used UUID versions (v0, v8, squuid)
   - ✗ Internal implementation details"
  (:refer-clojure :exclude [uuid? < > =])
  (:require
    [com.vadelabs.uid.flake.core :as flake]
    [com.vadelabs.uid.uuid.core :as uuid])
  (:import
    (java.util
      UUID)))


;; =============================================================================
;; UUID Constants
;; =============================================================================

(def +namespace-dns+ uuid/+namespace-dns+)
(def +namespace-url+ uuid/+namespace-url+)
(def +namespace-oid+ uuid/+namespace-oid+)
(def +namespace-x500+ uuid/+namespace-x500+)


;; =============================================================================
;; UUID Generation - Common Use Cases
;; =============================================================================

(defn v4
  "Generate a v4 (random) UUID using cryptographically secure randomness.

   This is the most commonly used UUID version for general-purpose unique identifiers.

   Example:
     (v4)  ;=> #uuid \"550e8400-e29b-41d4-a716-446655440000\""
  ^UUID []
  (uuid/v4))


(defn v7
  "Generate a v7 unix time-based, lexically sortable UUID.

   Recommended for database primary keys and distributed systems requiring
   time-ordering. Combines millisecond timestamp with random data and a
   monotonic counter for same-millisecond uniqueness.

   Example:
     (v7)  ;=> #uuid \"01890a5d-ac96-774b-bcce-b302099a8057\""
  ^UUID []
  (uuid/v7))


(defn v1
  "Generate a v1 (time-based) UUID with MAC address-based node identifier.

   Guaranteed unique and thread-safe. Less commonly used than v7 in modern
   applications due to privacy concerns (exposes MAC address).

   Example:
     (v1)  ;=> #uuid \"58e0a7d7-eebc-11d8-9669-0800200c9a66\""
  ^UUID []
  (uuid/v1))


(defn v6
  "Generate a v6 time-based, lexically sortable UUID.

   Field-compatible version of v1, reordered for improved database locality.
   Alternative to v7 when compatibility with v1 fields is needed.

   Example:
     (v6)  ;=> #uuid \"1d8eebc5-8e0a-67d7-9669-0800200c9a66\""
  ^UUID []
  (uuid/v6))


(defn v5
  "Generate a v5 (name-based, SHA1 hash) UUID for deterministic IDs.

   Always produces the same UUID for the same namespace and name.
   Useful for generating consistent identifiers from existing data.

   Args:
     namespace - A namespace UUID (use +namespace-dns+, +namespace-url+, etc.)
     name - The name to hash within the namespace

   Example:
     (v5 +namespace-dns+ \"example.com\")
     ;=> #uuid \"cfbff0d1-9375-5685-968c-48ce8b15ae17\""
  ^UUID [namespace name]
  (uuid/v5 namespace name))


(defn v3
  "Generate a v3 (name-based, MD5 hash) UUID for deterministic IDs.

   Similar to v5 but uses MD5 instead of SHA1. Prefer v5 for new applications.

   Args:
     namespace - A namespace UUID
     name - The name to hash within the namespace

   Example:
     (v3 +namespace-dns+ \"example.com\")
     ;=> #uuid \"9073926b-929f-31c2-abc9-fad77ae3e8eb\""
  ^UUID [namespace name]
  (uuid/v3 namespace name))


;; =============================================================================
;; UUID Core Operations
;; =============================================================================

(defn uuid?
  "Check if value is a UUID.

   Example:
     (uuid? (v4))  ;=> true
     (uuid? \"not-a-uuid\")  ;=> false"
  [x]
  (uuid/uuid? x))


(defn as-uuid
  "Parse or coerce value to UUID. Supports UUID strings, URN strings, and byte arrays.

   Example:
     (as-uuid \"550e8400-e29b-41d4-a716-446655440000\")
     ;=> #uuid \"550e8400-e29b-41d4-a716-446655440000\"

     (as-uuid \"urn:uuid:550e8400-e29b-41d4-a716-446655440000\")
     ;=> #uuid \"550e8400-e29b-41d4-a716-446655440000\""
  ^UUID [x]
  (uuid/as-uuid x))


(defn =
  "Compare UUIDs for equality. Can be used with multiple UUIDs.

   Example:
     (= (v4) (v4))  ;=> false (different random UUIDs)
     (= (v5 +namespace-dns+ \"x\") (v5 +namespace-dns+ \"x\"))  ;=> true"
  ([_] true)
  ([x y] (uuid/= x y))
  ([x y & more] (apply uuid/= x y more)))


(defn <
  "Compare UUIDs for < relation (lexical ordering).

   Example:
     (< (v7) (v7))  ;=> true (time-ordered)"
  ([_] true)
  ([x y] (uuid/< x y))
  ([x y & more] (apply uuid/< x y more)))


(defn >
  "Compare UUIDs for > relation (lexical ordering)."
  ([_] true)
  ([x y] (uuid/> x y))
  ([x y & more] (apply uuid/> x y more)))


;; =============================================================================
;; UUID Time Extraction (for time-based UUIDs only)
;; =============================================================================

(defn get-instant
  "Extract timestamp from time-based UUID as java.util.Date.

   Works with v1, v6, and v7 UUIDs. Returns nil for other versions.

   Example:
     (get-instant (v7))  ;=> #inst \"2024-07-10T15:30:45.123Z\""
  ^java.util.Date [uuid]
  (uuid/get-instant uuid))


(defn get-unix-time
  "Extract timestamp from time-based UUID as milliseconds since epoch.

   Works with v1, v6, and v7 UUIDs. Returns nil for other versions.

   Example:
     (get-unix-time (v7))  ;=> 1720626645123"
  [uuid]
  (uuid/get-unix-time uuid))


;; =============================================================================
;; Flake API - High-Performance Time-Ordered IDs
;; =============================================================================

(defn flake
  "Generate a new 192-bit time-ordered unique identifier with nanosecond precision.

   Flakes are monotonically increasing and lexically sortable. Ideal for distributed
   systems requiring nanosecond-precision timestamps and high uniqueness guarantees.

   Example:
     (flake)  ;=> #flake/id \"56S2f9LfyJJ52sY7JJuWL-etkrr2OBOC\""
  []
  (flake/flake))


(defn snowflake
  "Generate a new Flake directly as a URL-safe string.

   Returns a 32-character string that preserves monotonic ordering.
   More efficient than calling (str (flake)).

   Example:
     (snowflake)  ;=> \"56S2f9Lg0JJ52sY7JJuWL-etkrr2OBOC\""
  []
  (flake/flake-string))


(defn flake?
  "Check if value is a Flake.

   Example:
     (flake? (flake))  ;=> true
     (flake? \"not-a-flake\")  ;=> false"
  [x]
  (flake/flake? x))


(defn flake-time
  "Extract the nanosecond timestamp from a Flake.

   Returns nanoseconds since Unix epoch (January 1, 1970 00:00:00 UTC).

   Example:
     (flake-time (flake))  ;=> 1720626645123456789"
  [f]
  (flake/timestamp f))


(defn parse-flake
  "Parse a Flake from its string representation.

   Returns a Flake instance or nil if the string is invalid.

   Example:
     (parse-flake \"56S2f9Lg0JJ52sY7JJuWL-etkrr2OBOC\")
     ;=> #flake/id \"56S2f9Lg0JJ52sY7JJuWL-etkrr2OBOC\"

     (parse-flake \"invalid\")  ;=> nil"
  [s]
  (flake/from-string s))
