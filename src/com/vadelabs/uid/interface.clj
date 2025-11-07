(ns com.vadelabs.uid.interface
  "Unified interface for unique identifier generation.

   This component consolidates UUID (RFC9562) and Flake (192-bit time-ordered)
   identifier generation into a single namespace.

   ## UUID Support
   - v0-v8: Standard UUID versions following RFC9562
   - SQUUID: Sequential UUID for time-ordering

   ## Flake Support
   - High-performance 192-bit time-ordered identifiers
   - Nanosecond precision with 128-bit entropy

   Use the subnamespace aliases for specific functionality:
   - `uuid/*` for UUID operations
   - `flake/*` for Flake operations"
  (:refer-clojure :exclude [uuid? max < > =])
  (:require
    [com.vadelabs.uid.uuid.interface :as uuid]
    [com.vadelabs.uid.flake.interface :as flake]))

;; =============================================================================
;; UUID API - Re-export from uuid.interface
;; =============================================================================

;; Constants
(def +null+ uuid/+null+)
(def +max+ uuid/+max+)
(def +namespace-dns+ uuid/+namespace-dns+)
(def +namespace-url+ uuid/+namespace-url+)
(def +namespace-oid+ uuid/+namespace-oid+)
(def +namespace-x500+ uuid/+namespace-x500+)

;; UUID Generation
(def null uuid/null)
(def v0 uuid/v0)
(def max uuid/max)
(def v1 uuid/v1)
(def v3 uuid/v3)
(def v4 uuid/v4)
(def v5 uuid/v5)
(def v6 uuid/v6)
(def v7 uuid/v7)
(def v8 uuid/v8)
(def squuid uuid/squuid)

;; Time utilities
(def monotonic-time uuid/monotonic-time)

;; Protocols
(def UUIDNameBytes uuid/UUIDNameBytes)
(def UUIDable uuid/UUIDable)
(def UUIDRfc9562 uuid/UUIDRfc9562)
(def UUIDRfc4122 uuid/UUIDRfc4122)

;; Protocol functions
(def as-byte-array uuid/as-byte-array)
(def as-uuid uuid/as-uuid)
(def uuidable? uuid/uuidable?)
(def uuid? uuid/uuid?)
(def null? uuid/null?)
(def max? uuid/max?)
(def uuid= uuid/uuid=)
(def uuid< uuid/uuid<)
(def uuid> uuid/uuid>)

;; Comparison (note: these shadow clojure.core functions)
(def = uuid/=)
(def < uuid/<)
(def > uuid/>)

;; UUID accessors
(def get-version uuid/get-version)
(def get-variant uuid/get-variant)
(def get-word-high uuid/get-word-high)
(def get-word-low uuid/get-word-low)
(def get-time-low uuid/get-time-low)
(def get-time-mid uuid/get-time-mid)
(def get-time-high uuid/get-time-high)
(def get-clk-high uuid/get-clk-high)
(def get-clk-low uuid/get-clk-low)
(def get-timestamp uuid/get-timestamp)
(def get-instant uuid/get-instant)
(def get-unix-time uuid/get-unix-time)
(def get-node-id uuid/get-node-id)
(def get-clk-seq uuid/get-clk-seq)

;; Conversions
(def to-byte-array uuid/to-byte-array)
(def to-string uuid/to-string)
(def to-hex-string uuid/to-hex-string)
(def to-urn-string uuid/to-urn-string)
(def to-uri uuid/to-uri)

;; Predicates
(def uuid-string? uuid/uuid-string?)
(def uuid-urn-string? uuid/uuid-urn-string?)

;; =============================================================================
;; Flake API - Re-export from flake.interface
;; =============================================================================

(def flake flake/flake)
(def snowflake flake/snowflake)
(def flake-time flake/flake-time)
(def flake-hex flake/flake-hex)
(def flake? flake/flake?)
(def read-flake flake/read-method)
(def from-string flake/from-string)
(def make-flake flake/make-flake)
(def flake-bytes flake/flake-bytes)
