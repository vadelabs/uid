(ns com.vadelabs.uid.uuid.core
  "Core UUID generation and manipulation functions implementing RFC 9562.

  Provides comprehensive support for all UUID versions (v0-v8) with
  efficient internal representation and conversion utilities.

  Implementation inspired by clj-uuid by Dan Lentz:
  https://github.com/danlentz/clj-uuid
  Licensed under Eclipse Public License 1.0"
  (:refer-clojure :exclude [== uuid? max < > =])
  (:require
    [clojure.core :as clojure]
    [com.vadelabs.uid.uuid.bitmop    :as bitmop]
    [com.vadelabs.uid.uuid.clock     :as clock]
    [com.vadelabs.uid.uuid.constants :refer [+md5+ +sha1+ +ub32-mask+ uuid-regex urn-regex]]
    [com.vadelabs.uid.uuid.node      :as node]
    [com.vadelabs.uid.uuid.random    :as random]
    [com.vadelabs.uid.uuid.util      :as util])
  (:import
    (java.io
      ByteArrayOutputStream
      ObjectOutputStream)
    (java.lang
      IllegalArgumentException)
    (java.net
      URI)
    (java.nio
      ByteBuffer)
    (java.security
      MessageDigest)
    (java.util
      Date
      UUID)))


(def ^:const +null+
  "The NULL UUID is a special form of sentinel UUID that is specified to have
   all 128 bits set to zero."
  #uuid "00000000-0000-0000-0000-000000000000")


(def ^:const +max+
  "The MAX UUID is a special form of sentinel UUID that is specified to have
   all 128 bits set to one."
  #uuid "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF")


(def ^:const +namespace-dns+  #uuid "6ba7b810-9dad-11d1-80b4-00c04fd430c8")
(def ^:const +namespace-url+  #uuid "6ba7b811-9dad-11d1-80b4-00c04fd430c8")
(def ^:const +namespace-oid+  #uuid "6ba7b812-9dad-11d1-80b4-00c04fd430c8")
(def ^:const +namespace-x500+ #uuid "6ba7b814-9dad-11d1-80b4-00c04fd430c8")


(defn monotonic-time
  "Return a monotonic timestamp (guaranteed always increasing) based on
 the number of 100-nanosecond intervals elapsed since the adoption of
 the Gregorian calendar in the West, 12:00am Friday October 15, 1582 UTC."
  []
  (clock/monotonic-time))


(defprotocol UUIDNameBytes
  "A mechanism intended for user-level extension that defines the
  decoding rules for the local-part representation of arbitrary
  Clojure / Java Objects when used for computing namespaced
  identifiers."

  (as-byte-array
    [x]
    "Extract a byte serialization that represents the 'name' of x,
    typically unique within a given namespace."))


(defprotocol UUIDable
  "A UUIDable object directly represents a UUID."

  (as-uuid   ^java.util.UUID [x]
    "Coerce the value 'x' to a UUID.")

  (uuidable?
    [x]
    "Return 'true' if 'x' can be coerced to UUID."))


(defprotocol UUIDRfc9562
  "Protocol for RFC 9562 UUID operations."

  (hash-code [uuid]
    "Returns the hash code as a long.")

  (null? [uuid]
    "Returns true if this is the NULL UUID (all zeros).")

  (max? [uuid]
    "Returns true if this is the MAX UUID (all ones).")

  (uuid? [x]
    "Returns true if x is a UUID instance.")

  (uuid= [x y]
    "Returns true if UUIDs are equal.")

  (uuid< [x y]
    "Returns true if x < y (lexical byte-order comparison).")

  (uuid> [x y]
    "Returns true if x > y (lexical byte-order comparison).")

  (get-version [uuid]
    "Returns the version number (0-8).")

  (get-variant [uuid]
    "Returns the variant number (typically 2 for RFC 9562).")

  (get-timestamp [uuid]
    "Extracts timestamp from time-based UUIDs (v1/v6/v7).

     Returns:
     - v1/v6: 60-bit value (100-nanosecond intervals since Gregorian epoch 1582-10-15)
     - v7: 48-bit value (milliseconds since Unix epoch 1970-01-01)
     - Other versions: nil

     ## Bit Layout Diagrams

     ### UUID v1 (timestamp scattered across MSB)
     MSB (64 bits):
     ┌─────────────────────────────────┬────────────────────┬──────────────┬──────┐
     │        time_low (32 bits)       │  time_mid (16 bits)│ time_hi (12) │ ver  │
     │  [bits 0-31 of timestamp]       │  [bits 32-47]      │ [bits 48-59] │ (4)  │
     └─────────────────────────────────┴────────────────────┴──────────────┴──────┘
     Extract: time_low || time_mid || time_hi → 60-bit timestamp

     ### UUID v6 (timestamp reordered for lexical sorting)
     MSB (64 bits):
     ┌──────────────────┬────────────────────┬──────────────┬──────┐
     │  time_high (32)  │  time_mid (16)     │ time_low (12)│ ver  │
     │  [bits 28-59]    │  [bits 12-27]      │ [bits 0-11]  │ (4)  │
     └──────────────────┴────────────────────┴──────────────┴──────┘
     Bits: [63──────────────────────────16][15────────────12][11─8][7────0]
     Extract: time_hi(28) || time_mid(12) || time_lo(12) → 60-bit timestamp

     ### UUID v7 (Unix timestamp at start for optimal sorting)
     MSB (64 bits):
     ┌─────────────────────────────────────────────────┬──────┬──────────────┐
     │          unix_ts_ms (48 bits)                   │ ver  │  rand (12)   │
     │     milliseconds since Unix epoch 1970-01-01    │ (4)  │              │
     └─────────────────────────────────────────────────┴──────┴──────────────┘
     Bits: [63──────────────────────────────16][15──12][11────────────────0]
     Extract: bits [63:16] → 48-bit timestamp")

  (get-instant ^java.util.Date [uuid]
    "Returns timestamp as java.util.Date, or nil for non-time-based UUIDs.")

  (get-unix-time [uuid]
    "Returns timestamp as milliseconds since Unix epoch, or nil for non-time-based UUIDs.")

  ;; Structured accessors
  (get-words [uuid]
    "Returns UUID as two 64-bit words: {:high long :low long}")

  (get-time-fields [uuid]
    "Returns time components: {:low long :mid long :high long}
     Layout varies by version (v1 vs v6).")

  (get-clock-fields [uuid]
    "Returns clock components: {:seq short :high long :low long}
     seq is non-nil only for v1/v6.")

  (get-node [uuid]
    "Returns node identifier: {:id long}")

  ;; Conversions
  (to-byte-array [uuid]
    "Returns 16-byte array representation.")

  (to-string ^String [uuid]
    "Returns canonical string: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")

  (to-hex-string ^String [uuid]
    "Returns 32-character hex string (no hyphens).")

  (to-urn-string ^String [uuid]
    "Returns URN format: urn:uuid:xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")

  (to-uri ^java.net.URI [uuid]
    "Returns UUID as java.net.URI in URN format."))


(def UUIDRfc4122 UUIDRfc9562)


(extend-type UUID
  UUIDable
  (as-uuid   [u] u)
  (uuidable? [_] true)

  UUIDRfc9562
  (uuid? ^boolean [_] true)

  (uuid= ^boolean [^UUID x ^UUID y]
    (.equals x y))

  (uuid< ^boolean [^UUID x ^UUID y]
    (let [xh (.getMostSignificantBits x)
          yh (.getMostSignificantBits y)
          c  (Long/compareUnsigned xh yh)]
      (if (clojure/= c 0)
        (clojure/< (Long/compareUnsigned (.getLeastSignificantBits x)
                                         (.getLeastSignificantBits y)) 0)
        (clojure/< c 0))))

  (uuid> ^boolean [^UUID x ^UUID y]
    (let [xh (.getMostSignificantBits x)
          yh (.getMostSignificantBits y)
          c  (Long/compareUnsigned xh yh)]
      (if (clojure/= c 0)
        (clojure/> (Long/compareUnsigned (.getLeastSignificantBits x)
                                         (.getLeastSignificantBits y)) 0)
        (clojure/> c 0))))

  (null? ^boolean [uuid]
    (clojure/= 0 (.getMostSignificantBits uuid) (.getLeastSignificantBits uuid)))

  (max? ^boolean [uuid]
    (uuid= uuid +max+))

  (to-byte-array ^bytes [uuid]
    (let [arr (byte-array 16)]
      (bitmop/long->bytes (.getMostSignificantBits  uuid) arr 0)
      (bitmop/long->bytes (.getLeastSignificantBits uuid) arr 8)
      arr))

  (hash-code ^long [uuid]
    (long (.hashCode uuid)))

  (get-version ^int [uuid]
    (.version uuid))

  (get-variant ^int [uuid]
    (.variant uuid))

  (to-string [uuid]
    (.toString uuid))

  (to-urn-string [uuid]
    (str "urn:uuid:" (.toString uuid)))

  (to-hex-string [uuid]
    (let [{:keys [high low]} (get-words uuid)]
      (str (bitmop/hex high) (bitmop/hex low))))

  (to-uri [uuid]
    (URI/create (to-urn-string uuid)))

  (get-timestamp ^long [uuid]
    (case (.version uuid)
      ;; v1: Java UUID provides native .timestamp() method
      1 (.timestamp uuid)

      ;; v6: Reassemble 60-bit timestamp from reordered fields
      ;; MSB layout: [time_high(32) | time_mid(16) | time_low(12) | ver(4)]
      ;; Extract time_low (bits 12-15), then shift and OR with mid/high
      ;; Result: time_high(28) || time_mid(16) || time_low(12) = 60 bits
      6 (let [{:keys [mid high]} (get-time-fields uuid)]
          (bit-or (bitmop/ldb #=(bitmop/mask 12 0)
                              (.getMostSignificantBits uuid))
                  (bit-shift-left mid 12)
                  (bit-shift-left high 28)))

      ;; v7: Extract 48-bit Unix timestamp from MSB
      ;; MSB layout: [unix_ts_ms(48) | ver(4) | rand(12)]
      ;; Extract bits [63:16] using mask-and-shift
      7 (bitmop/ldb #=(bitmop/mask 48 16) (.getMostSignificantBits uuid))

      nil))

  (get-unix-time ^long [uuid]
    (case (.version uuid)
      (1 6) (clock/posix-time (get-timestamp uuid))
      7     (get-timestamp uuid)
      nil))

  (get-instant [uuid]
    (when-let [ts (get-unix-time uuid)]
      (Date. (long ts))))

  ;; Structured accessors
  (get-words [uuid]
    {:high (.getMostSignificantBits uuid)
     :low  (.getLeastSignificantBits uuid)})

  (get-time-fields [uuid]
    (let [msb (.getMostSignificantBits uuid)
          version (get-version uuid)]
      (if (clojure/= 6 version)
        ;; v6 field layout (reordered for sorting):
        ;; MSB: [time_high(32) | time_mid(16) | time_low(12) | ver(4)]
        ;;      [63────────32][31─────────16][15─────────12][11──8]
        {:low  (bitmop/ldb #=(bitmop/mask 16 0) msb)        ; Extract bits [15:0] (includes time_low + ver)
         :mid  (bitmop/ldb #=(bitmop/mask 16 16) msb)       ; Extract bits [31:16] (time_mid)
         :high (bitmop/ldb #=(bitmop/mask 32 0) (bit-shift-right msb 32))} ; Extract bits [63:32] (time_high)

        ;; v1 and v7 field layout (original RFC 4122 order):
        ;; MSB: [time_low(32) | time_mid(16) | time_high(12) | ver(4)]
        ;;      [63───────32][31──────────16][15──────────12][11──8]
        {:low  (bitmop/ldb #=(bitmop/mask 32 0) (bit-shift-right msb 32)) ; Extract bits [63:32] (time_low)
         :mid  (bitmop/ldb #=(bitmop/mask 16 16) msb)                     ; Extract bits [31:16] (time_mid)
         :high (bitmop/ldb #=(bitmop/mask 16 0) msb)})))

  (get-clock-fields [uuid]
    ;; LSB layout for v1/v6:
    ;; [clk_seq_hi_and_res(8) | clk_seq_low(8) | node(48)]
    ;; [63───────────────56][55──────────48][47─────────0]
    ;;
    ;; For v7 and others, the fields have different meaning but same extraction
    (let [lsb (.getLeastSignificantBits uuid)]
      {:seq  (when (#{1 6} (.version uuid))
               (.clockSequence uuid))              ; Java UUID provides clock sequence for v1/v6
       :high (bitmop/ldb #=(bitmop/mask 8 48) lsb) ; Extract bits [55:48] (clk_seq_low)
       :low  (bitmop/ldb #=(bitmop/mask 8 0) (bit-shift-right lsb 56))}))

  (get-node [uuid]
    ;; Extract node identifier from LSB
    ;; LSB: [clk_seq_hi(8) | clk_seq_low(8) | node(48)]
    ;;      [63──────────56][55───────────48][47───────0]
    {:id (bitmop/ldb #=(bitmop/mask 48 0)
                     (.getLeastSignificantBits uuid))}) ; Extract bits [47:0] (node identifier)

  UUIDNameBytes
  (as-byte-array ^bytes [this]
    (to-byte-array this)))


;; =============================================================================
;; UUID Generation Functions
;; =============================================================================

(defn null
  "Generates the v0 (null) UUID."
  ^java.util.UUID
  []
  +null+)


(defn v0
  "Generates the v0 (null) UUID."
  ^java.util.UUID
  []
  +null+)


(defn max
  "Generates the v15 (maximum) UUID."
  ^java.util.UUID
  []
  +max+)


(defn v1
  "Generate a v1 (time-based) unique identifier."
  ^java.util.UUID
  []
  (let [ts        (clock/monotonic-time)
        time-low  (bitmop/ldb #=(bitmop/mask 32  0)  ts)
        time-mid  (bitmop/ldb #=(bitmop/mask 16 32)  ts)
        time-high (bitmop/dpb #=(bitmop/mask 4  12)
                              (bitmop/ldb #=(bitmop/mask 12 48) ts) 0x1)
        msb       (bit-or time-high
                          (bit-shift-left time-low 32)
                          (bit-shift-left time-mid 16))]
    (UUID. msb @node/+v1-lsb+)))


(defn v6
  "Generate a v6 (time-based), lexically sortable, unique identifier."
  ^java.util.UUID
  []
  (let [ts        (clock/monotonic-time)
        time-high (bitmop/ldb #=(bitmop/mask 32 28) ts)
        time-mid  (bitmop/ldb #=(bitmop/mask 16 12) ts)
        time-low  (bitmop/dpb #=(bitmop/mask 4  12)
                              (bitmop/ldb #=(bitmop/mask 12 0) ts) 0x6)
        msb       (bit-or time-low
                          (bit-shift-left time-mid  16)
                          (bit-shift-left time-high 32))]
    (UUID. msb @node/+v6-lsb+)))


(defn v7
  "Generate a v7 unix time-based, lexically sortable UUID."
  ^java.util.UUID
  []
  (let [[t counter]     (clock/monotonic-unix-time-and-random-counter)
        timestamp       (bitmop/ldb #=(bitmop/mask 48  0) t)
        ver-and-counter (bitmop/dpb #=(bitmop/mask 4  12) counter 0x7)
        msb             (bit-or ver-and-counter (bit-shift-left timestamp 16))
        lsb             (bitmop/dpb #=(bitmop/mask 2 62) (random/long) 0x2)]
    (UUID. msb lsb)))


(defn v4
  "Generate a v4 (random) UUID."
  (^java.util.UUID []
   (UUID/randomUUID))
  (^java.util.UUID [msb lsb]
   (UUID.
     (bitmop/dpb #=(bitmop/mask 4 12) msb 0x4)
     (bitmop/dpb #=(bitmop/mask 2 62) lsb 0x2))))


(defn v8
  "Generate a v8 custom UUID with up to 122 bits of user data."
  ^java.util.UUID
  [^long msb ^long lsb]
  (UUID.
    (bitmop/dpb #=(bitmop/mask 4 12) msb 0x8)
    (bitmop/dpb #=(bitmop/mask 2 62) lsb 0x2)))


(defn squuid
  "Generate a SQUUID (sequential, random) unique identifier."
  ^java.util.UUID
  []
  (let [uuid (v4)
        secs (clock/posix-time)
        {:keys [high low]} (get-words uuid)
        timed-msb (bit-or (bit-shift-left secs 32)
                          (bit-and +ub32-mask+ high))]
    (UUID. timed-msb low)))


(def ^:private byte-array-class (Class/forName "[B"))


(extend-protocol UUIDNameBytes
  java.lang.Object
  (as-byte-array ^bytes [this]
    (if (instance? byte-array-class this)
      this
      (let [baos (ByteArrayOutputStream.)
            oos  (ObjectOutputStream. baos)]
        (.writeObject oos this)
        (.close oos)
        (.toByteArray baos))))

  java.lang.String
  (as-byte-array ^bytes [this]
    (util/compile-if (util/java6?)
                     (.getBytes this)
                     (.getBytes this java.nio.charset.StandardCharsets/UTF_8)))

  java.net.URL
  (as-byte-array ^bytes [this]
    (as-byte-array (.toString this)))

  nil
  (as-byte-array [x]
    (throw (IllegalArgumentException. (format "%s cannot be converted to byte array." x)))))


(defn- make-digest
  ^java.security.MessageDigest
  [^String designator]
  (MessageDigest/getInstance designator))


(defn- digest-bytes
  ^bytes
  [^String kind ^bytes ns-bytes ^bytes local-bytes]
  (let [m (make-digest kind)]
    (.update m ns-bytes)
    (.digest m local-bytes)))


(defn- build-digested-uuid
  ^java.util.UUID
  [^long version ^bytes arr]
  {:pre [(or (clojure/= version 3) (clojure/= version 5))]}
  (let [msb (bitmop/bytes->long arr 0)
        lsb (bitmop/bytes->long arr 8)]
    (UUID.
      (bitmop/dpb #=(bitmop/mask 4 12) msb version)
      (bitmop/dpb #=(bitmop/mask 2 62) lsb 0x2))))


(defn v3
  "Generate a v3 (name based, MD5 hash) UUID."
  ^java.util.UUID
  [context local-part]
  (build-digested-uuid 3
                       (digest-bytes +md5+
                                     (to-byte-array (as-uuid context))
                                     (as-byte-array local-part))))


(defn v5
  "Generate a v5 (name based, SHA1 hash) UUID."
  ^java.util.UUID
  [context local-part]
  (build-digested-uuid 5
                       (digest-bytes +sha1+
                                     (to-byte-array (as-uuid context))
                                     (as-byte-array local-part))))


(defn =
  "Directly compare two or more UUIDs for equality."
  ([_] true)
  ([x y]
   (uuid= x y))
  ([x y & more]
   (every? (fn [[a b]] (uuid= a b))
           (partition 2 1 (list* x y more)))))


(defn >
  "Directly compare two or more UUIDs for > relation."
  ([_] true)
  ([x y]
   (uuid> x y))
  ([x y & more]
   (every? (fn [[a b]] (uuid> a b))
           (partition 2 1 (list* x y more)))))


(defn <
  "Directly compare two or more UUIDs for < relation."
  ([_] true)
  ([x y]
   (uuid< x y))
  ([x y & more]
   (every? (fn [[a b]] (uuid< a b))
           (partition 2 1 (list* x y more)))))


(defn uuid-string?
  [s]
  (and (string? s)
       (some? (re-matches uuid-regex s))))


(defn uuid-urn-string?
  [s]
  (and (string? s)
       (some? (re-matches urn-regex s))))


(defn uuid-vec?
  [v]
  (and (= (count v) 16)
       (every? #(and (integer? %) (<= -128 % 127)) v)))


(defn- str->uuid
  [s]
  (cond
    (uuid-string?     s) (UUID/fromString s)
    (uuid-urn-string? s) (UUID/fromString (subs s 9))
    :else                (throw (IllegalArgumentException. (format "Invalid UUID: %s" s)))))


;; Object and nil only need uuid? predicate, not the full protocol
#_{:clj-kondo/ignore [:missing-protocol-method]}


(extend-protocol UUIDRfc9562
  Object
  (uuid? [_] false)

  nil
  (uuid? [_] false))


(extend-protocol UUIDable
  byte/1
  (as-uuid [^bytes ba]
    (let [bb (ByteBuffer/wrap ba)]
      (UUID. (.getLong bb) (.getLong bb))))
  (uuidable? [^bytes ba]
    (clojure/= 16 (alength ^bytes ba)))

  String
  (uuidable? ^boolean [s]
    (or
      (uuid-string?     s)
      (uuid-urn-string? s)))
  (as-uuid [s]
    (str->uuid s))

  URI
  (uuidable? ^boolean [u]
    (uuid-urn-string? (str u)))
  (as-uuid [u]
    (str->uuid (str u)))

  Object
  (uuidable? ^boolean [_]
    false)
  (as-uuid [_x]
    (throw (IllegalArgumentException. (format "%s Cannot be coerced to UUID." _x))))

  nil
  (as-uuid [_x]
    (throw (IllegalArgumentException. (format "%s cannot be coerced to UUID." _x))))
  (uuidable? ^boolean [_] false))
