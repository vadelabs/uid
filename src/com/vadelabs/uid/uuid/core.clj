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
  "A protocol that abstracts an unique identifier as described by
  IETF RFC9562."

  (hash-code                     [uuid])

  (null?                         [uuid])

  (max?                         [uuid])

  (uuid?                         [x])

  (uuid=                         [x y])

  (uuid<                         [x y])

  (uuid>                         [x y])

  (get-word-high                 [uuid])

  (get-word-low                  [uuid])

  (get-version                   [uuid])

  (get-variant                   [uuid])

  (get-time-low                  [uuid])

  (get-time-mid                  [uuid])

  (get-time-high                 [uuid])

  (get-clk-high                  [uuid])

  (get-clk-low                   [uuid])

  (get-clk-seq                   [uuid])

  (get-node-id                   [uuid])

  (get-timestamp                 [uuid])

  (get-instant   ^java.util.Date [uuid])

  (get-unix-time                 [uuid])

  (to-byte-array                 [uuid])

  (to-string     ^String         [uuid])

  (to-hex-string ^String         [uuid])

  (to-urn-string ^String         [uuid])

  (to-uri        ^java.net.URI   [uuid]))


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

  (get-word-high ^long [uuid]
    (.getMostSignificantBits uuid))

  (get-word-low ^long [uuid]
    (.getLeastSignificantBits uuid))

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
    (str (bitmop/hex (get-word-high uuid)) (bitmop/hex (get-word-low uuid))))

  (to-uri [uuid]
    (URI/create (to-urn-string uuid)))

  (get-time-low ^long [uuid]
    (let [msb (.getMostSignificantBits uuid)]
      (if (clojure/= 6 (get-version uuid))
        (bitmop/ldb #=(bitmop/mask 16 0) msb)
        (bitmop/ldb #=(bitmop/mask 32 0) (bit-shift-right msb 32)))))

  (get-time-mid ^long [uuid]
    (bitmop/ldb #=(bitmop/mask 16 16)
                (.getMostSignificantBits uuid)))

  (get-time-high ^long [uuid]
    (let [msb (.getMostSignificantBits uuid)]
      (if (clojure/= 6 (get-version uuid))
        (bitmop/ldb #=(bitmop/mask 32 0) (bit-shift-right msb 32))
        (bitmop/ldb #=(bitmop/mask 16 0) msb))))

  (get-clk-low ^long [uuid]
    (bitmop/ldb #=(bitmop/mask 8 0)
                (bit-shift-right (.getLeastSignificantBits uuid) 56)))

  (get-clk-high ^long [uuid]
    (bitmop/ldb #=(bitmop/mask 8 48)
                (.getLeastSignificantBits uuid)))

  (get-clk-seq ^short [uuid]
    (when (#{1 6} (.version uuid))
      (.clockSequence uuid)))

  (get-node-id ^long [uuid]
    (bitmop/ldb #=(bitmop/mask 48 0)
                (.getLeastSignificantBits uuid)))

  (get-timestamp ^long [uuid]
    (case (.version uuid)
      1 (.timestamp uuid)
      6 (bit-or (bitmop/ldb #=(bitmop/mask 12 0)
                            (.getMostSignificantBits uuid))
                (bit-shift-left (get-time-mid uuid) 12)
                (bit-shift-left (get-time-high uuid) 28))
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

  UUIDNameBytes
  (as-byte-array ^bytes [this]
    (to-byte-array this)))


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
    (UUID. msb (node/+v1-lsb+))))


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
    (UUID. msb (node/+v6-lsb+))))


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
        lsb  (get-word-low  uuid)
        msb  (get-word-high uuid)
        timed-msb (bit-or (bit-shift-left secs 32)
                          (bit-and +ub32-mask+ msb))]
    (UUID. timed-msb lsb)))


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


(defn- compare-many
  [f x y more]
  (if (f x y)
    (if (next more)
      (recur f y (first more) (next more))
      (f y (first more)))
    false))


(defn =
  "Directly compare two or more UUIDs for equality."
  ([_] true)
  ([x y]
   (uuid= x y))
  ([x y & more]
   (compare-many uuid= x y more)))


(defn >
  "Directly compare two or more UUIDs for > relation."
  ([_] true)
  ([x y]
   (uuid> x y))
  ([x y & more]
   (compare-many uuid> x y more)))


(defn <
  "Directly compare two or more UUIDs for < relation."
  ([_] true)
  ([x y]
   (uuid< x y))
  ([x y & more]
   (compare-many uuid< x y more)))


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
  (and (clojure/= (count v) 16)
       (every? #(and (integer? %) (>= -128  % 127)) v)))


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


