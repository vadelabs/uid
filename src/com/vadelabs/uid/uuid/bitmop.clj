(ns com.vadelabs.uid.uuid.bitmop
  (:refer-clojure :exclude [< + - * bit-and bit-or bit-shift-left bit-shift-right
                            byte int long dec inc zero?])
  (:require
    [clj-commons.primitive-math :refer [< + - * bit-and bit-or bit-shift-left bit-shift-right
                                        >>> byte int long dec inc zero?]]
    [com.vadelabs.uid.uuid.constants :refer [+hex-chars+ +ub4-mask+ +ub8-mask+ +ub16-mask+
                                             +ub24-mask+ +ub32-mask+ +ub48-mask+ +ub56-mask+]]))


(defn expt2
  ^long
  [^long pow]
  (bit-set 0 pow))


(defn mask
  ^long
  [^long width ^long offset]
  (if (< (+ width offset) 64)
    (bit-shift-left (dec (bit-shift-left 1 width)) offset)
    (let [x (expt2 offset)]
      (bit-and-not -1 (dec x)))))


(declare mask-offset mask-width)


(defn mask-offset
  ^long
  [^long m]
  (cond
    (zero? m) 0
    (neg?  m) (- 64 ^long (mask-width m))
    :else     (loop [c 0]
                (if (pos? (bit-and 1 (bit-shift-right m c)))
                  c
                  (recur (inc c))))))


(defn mask-width
  ^long
  [^long m]
  (if (neg? m)
    (let [x (mask-width (- (inc m)))]
      (- 64 x))
    (loop [m (bit-shift-right m (mask-offset m)) c 0]
      (if (zero? (bit-and 1 (bit-shift-right m c)))
        c
        (recur m (inc c))))))


(defn ldb
  "Load Byte"
  ^long
  [^long bitmask ^long n]
  (let [off (mask-offset bitmask)]
    (bit-and (>>> bitmask off)
             (bit-shift-right n off))))


(defn dpb
  "Deposit Byte"
  ^long
  [^long bitmask ^long n ^long value]
  (bit-or (bit-and-not n bitmask)
          (bit-and bitmask
                   (bit-shift-left value (mask-offset bitmask)))))


(defn bit-count
  ^long
  [^long x]
  (let [n (ldb #=(mask 63 0) x) s (if (neg? x) 1 0)]
    (loop [c s i 0]
      (if (zero? (bit-shift-right n i))
        c
        (recur (+ c (bit-and 1 (bit-shift-right n i))) (inc i))))))


(defn ub4
  [n]
  (byte (bit-and n +ub4-mask+)))


(defn ub8
  [^long n]
  (unchecked-short (bit-and n +ub8-mask+)))


(defn ub16
  [n]
  (int (bit-and n +ub16-mask+)))


(defn ub24
  [n]
  (int (bit-and n +ub24-mask+)))


(defn ub32
  [n]
  (long (bit-and n +ub32-mask+)))


(defn ub48
  [n]
  (long (bit-and n +ub48-mask+)))


(defn ub56
  [n]
  (long (bit-and n +ub56-mask+)))


(defn sb8
  [n]
  (unchecked-byte (ub8 n)))


(defn sb16
  [n]
  (unchecked-short (ub16 n)))


(defn sb32
  [n]
  (unchecked-int (ub32 n)))


(defn sb64
  [n]
  (unchecked-long n))


(defn assemble-bytes
  [v]
  (loop [tot 0 bs v c 8]
    (if (zero? c)
      tot
      (recur
        (long (dpb (mask 8 (* 8 (dec c))) tot ^long (first bs)))
        (rest bs)
        (dec c)))))


(defn bytes->long
  [^bytes arr ^long i]
  (loop [tot 0 j i c 8]
    (if (zero? c)
      tot
      (recur
        (long (dpb (mask 8 (* 8 ^long (dec c))) tot  (aget arr j)))
        (inc j)
        (dec c)))))


(defn long->bytes
  ([^long x]
   (long->bytes x (byte-array 8) 0))
  ([^long x ^bytes arr ^long i]
   (loop [j 7 k 0]
     (if (neg? j)
       arr
       (do
         (aset-byte arr (+ i k) (sb8 (ldb (mask 8 (* 8 j)) x)))
         (recur (dec j) (inc k)))))))


(defn octet-hex
  [n]
  (str
    (+hex-chars+ (bit-shift-right n 4))
    (+hex-chars+ (bit-and 0x0F n))))


(defn hex
  [thing]
  (if (number? thing)
    (hex (map ub8 (long->bytes thing)))
    (apply str (map octet-hex thing))))
