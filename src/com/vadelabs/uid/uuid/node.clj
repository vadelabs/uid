(ns com.vadelabs.uid.uuid.node
  (:require
    [com.vadelabs.uid.uuid.bitmop :refer [assemble-bytes ldb dpb mask]]
    [com.vadelabs.uid.uuid.random :as random]
    [com.vadelabs.uid.uuid.util   :refer [java6? compile-if]])
  (:import
    (java.net
      InetAddress
      NetworkInterface)
    (java.security
      MessageDigest)
    (java.util
      Properties)))


(defonce +clock-sequence+ (inc (rand-int 0xffff)))


(def ^:private datasources
  ["java.vendor"
   "java.vendor.url"
   "java.version"
   "os.arch"
   "os.name"
   "os.version"])


(defn- all-local-addresses
  []
  (let [^InetAddress local-host (InetAddress/getLocalHost)
        host-name (.getCanonicalHostName local-host)
        base-addresses #{(str local-host) host-name}
        network-interfaces (reduce (fn [acc ^NetworkInterface ni]
                                     (concat acc
                                             (map str (enumeration-seq
                                                        (.getInetAddresses ni)))))
                                   base-addresses
                                   (enumeration-seq
                                     (NetworkInterface/getNetworkInterfaces)))]
    (reduce conj network-interfaces
            (map str (InetAddress/getAllByName host-name)))))


(defn- make-node-id
  []
  (let [addresses (all-local-addresses)
        ^MessageDigest digest (MessageDigest/getInstance "MD5")
        ^Properties    props  (System/getProperties)
        to-digest (reduce (fn [acc k]
                            (conj acc (.getProperty props k)))
                          addresses datasources)]
    (doseq [^String d to-digest]
      (compile-if (java6?)
                  (.update digest (.getBytes d))
                  (.update digest
                           (.getBytes d java.nio.charset.StandardCharsets/UTF_8))))
    (map bit-or
         [0x00 0x00 0x00 0x00 0x00 0x01]
         (take 6 (seq (.digest digest))))))


(def node-id make-node-id)

(def +node-id+ (delay (assemble-bytes (cons 0 (cons 0 (node-id))))))


(defn- make-lsb
  "Constructs the least significant bits for v1/v6 UUIDs.

   LSB layout (64 bits):
   [clk_seq_hi_and_res(8) | clk_seq_low(8) | node(48)]
   [63──────────────56][55──────────48][47───────0]

   The variant bits (2 bits at position 62-63) are set to 0x2 (10 in binary)
   to indicate RFC 9562 compliance."
  [node-value]
  (let [clk-high  (dpb (mask 2 6) (ldb (mask 6 8) +clock-sequence+) 0x2)
        clk-low   (ldb (mask 8 0) +clock-sequence+)]
    (dpb (mask 8 56) (dpb (mask 8 48) node-value clk-low) clk-high)))


(def +v1-lsb+
  "Precomputed LSB for v1 UUIDs using MAC address-based node identifier."
  (delay (make-lsb @+node-id+)))


(def +v6-lsb+
  "Precomputed LSB for v6 UUIDs using random node identifier."
  (delay (make-lsb (random/long))))
