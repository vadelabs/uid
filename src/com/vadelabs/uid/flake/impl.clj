(ns com.vadelabs.uid.flake.impl
  "High-performance time-ordered unique identifier implementation.

  A Flake is a 192-bit identifier combining:
  - 64-bit nanosecond-precision timestamp
  - 128-bit random component

  Designed for distributed systems requiring:
  - Monotonic ordering properties
  - High collision resistance
  - Efficient string serialization

  Implementation inspired by μ/log (mulog) by Bruno Bonacci:
  https://github.com/BrunoBonacci/mulog/blob/master/mulog-core/src/com/brunobonacci/mulog/flakes.clj
  Licensed under Apache License 2.0"
  (:require
   [com.vadelabs.uid.flake.nanoclock :as clock])
  (:import
   [java.util Random]))

(defn- throw-invalid-char!
  "Throws an exception for invalid character in encoded string."
  [encoded position]
  (throw (ex-info "Invalid character in encoded string"
                  {:encoded encoded :position position})))

(def ^:private encoding-alphabet "-0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz")
(def ^:private encoding-chars (vec encoding-alphabet))
(def ^:private encoded-length 32)
(def ^:private byte-length 24)

(def ^:private ^ThreadLocal rng-thread-local
  (ThreadLocal/withInitial
    (reify java.util.function.Supplier
      (get [_] (Random.)))))

(defn- long->bytes
  [^long value bs offset]
  (loop [i 7
         v value]
    (when (>= i 0)
      (aset-byte bs (+ offset i) (unchecked-byte (bit-and v 0xFF)))
      (recur (dec i) (unsigned-bit-shift-right v 8)))))

(defn- bytes->long
  [bs offset]
  (loop [i 0
         value 0]
    (if (< i 8)
      (recur (inc i)
             (bit-or (bit-shift-left value 8)
                     (bit-and (aget bs (+ offset i)) 0xFF)))
      value)))

(defn- encode->string
  [bs]
  (let [sb (StringBuilder. encoded-length)]
    (loop [i 0]
      (when (< i (alength bs))
        (let [b1 (bit-and (aget bs i) 0xFF)
              b2 (if (< (+ i 1) (alength bs))
                   (bit-and (aget bs (+ i 1)) 0xFF)
                   0)
              b3 (if (< (+ i 2) (alength bs))
                   (bit-and (aget bs (+ i 2)) 0xFF)
                   0)
              triplet (bit-or (bit-shift-left b1 16)
                              (bit-shift-left b2 8)
                              b3)]
          (.append sb (nth encoding-chars (bit-and (unsigned-bit-shift-right triplet 18) 0x3F)))
          (.append sb (nth encoding-chars (bit-and (unsigned-bit-shift-right triplet 12) 0x3F)))
          (.append sb (nth encoding-chars (bit-and (unsigned-bit-shift-right triplet 6) 0x3F)))
          (.append sb (nth encoding-chars (bit-and triplet 0x3F)))
          (recur (+ i 3)))))
    (.toString sb)))

(defn- decode<-string
  [^String encoded]
  (let [result (byte-array (quot (* (count encoded) 3) 4))]
    (loop [i 0
           pos 0]
      (if (< i (count encoded))
        (let [c1 (.indexOf encoding-alphabet (int (.charAt encoded i)))
              c2 (.indexOf encoding-alphabet (int (.charAt encoded (+ i 1))))
              c3 (.indexOf encoding-alphabet (int (.charAt encoded (+ i 2))))
              c4 (.indexOf encoding-alphabet (int (.charAt encoded (+ i 3))))]
          (when (or (= -1 c1) (= -1 c2) (= -1 c3) (= -1 c4))
            (throw-invalid-char! encoded i))
          (let [triplet (bit-or (bit-shift-left c1 18)
                                (bit-shift-left c2 12)
                                (bit-shift-left c3 6)
                                c4)]
            (when (< pos (alength result))
              (aset-byte result pos (unchecked-byte (bit-and (unsigned-bit-shift-right triplet 16) 0xFF))))
            (when (< (+ pos 1) (alength result))
              (aset-byte result (+ pos 1) (unchecked-byte (bit-and (unsigned-bit-shift-right triplet 8) 0xFF))))
            (when (< (+ pos 2) (alength result))
              (aset-byte result (+ pos 2) (unchecked-byte (bit-and triplet 0xFF))))
            (recur (+ i 4) (+ pos 3))))
        result))))

(defrecord Flake [timestamp random-high random-low]
  Object
  (toString [_]
    (let [bs (byte-array byte-length)]
      (long->bytes timestamp bs 0)
      (long->bytes random-high bs 8)
      (long->bytes random-low bs 16)
      (encode->string bs)))

  Comparable
  (compareTo [_ other]
    (let [cmp (compare timestamp (:timestamp other))]
      (if (not= 0 cmp)
        cmp
        (let [cmp (compare random-high (:random-high other))]
          (if (not= 0 cmp)
            cmp
            (compare random-low (:random-low other))))))))

(defn flake
  []
  (let [current-time (clock/current-time-nanos)
        ^Random rng (.get rng-thread-local)]
    (->Flake current-time (.nextLong rng) (.nextLong rng))))

(defn parse-flake
  [flake-str]
  (when (and flake-str (= encoded-length (count flake-str)))
    (try
      (let [bs (decode<-string flake-str)]
        (when (= byte-length (alength bs))
          (->Flake (bytes->long bs 0)
                   (bytes->long bs 8)
                   (bytes->long bs 16))))
      (catch Exception _
        nil))))

(defn make-flake
  ([timestamp-nanos random-high random-low]
   (->Flake timestamp-nanos random-high random-low))
  ([byte-data]
   (when (and byte-data (= byte-length (alength byte-data)))
     (->Flake (bytes->long byte-data 0)
              (bytes->long byte-data 8)
              (bytes->long byte-data 16)))))

(defn flake->bytes
  [^Flake f]
  (let [bs (byte-array byte-length)]
    (long->bytes (:timestamp f) bs 0)
    (long->bytes (:random-high f) bs 8)
    (long->bytes (:random-low f) bs 16)
    bs))

(defn flake->string
  [^Flake f]
  (.toString f))

(defn flake->hex
  [^Flake f]
  (format "%016x%016x%016x"
          (:timestamp f)
          (:random-high f)
          (:random-low f)))

(defn timestamp-nanos
  [^Flake f]
  (:timestamp f))

(defn flake?
  [x]
  (instance? Flake x))