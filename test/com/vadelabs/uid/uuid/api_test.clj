(ns com.vadelabs.uid.uuid.api-test
  (:refer-clojure :exclude [uuid? max])
  (:require
    [clojure.test :refer [deftest is testing]]
    [com.vadelabs.uid.uuid.core :as uuid])
  (:import
    (java.lang
      IllegalArgumentException)))


(deftest check-unique-identifier-protocol
  (testing "v0 uuid protocol..."
    (let [tmpid uuid/+null+]
      (is (= (uuid/get-word-high tmpid)       0))
      (is (= (uuid/get-word-low tmpid)        0))
      (is (= (uuid/null? tmpid)           true))
      (is (= (seq (uuid/to-byte-array tmpid)) [0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0]))
      (is (= (uuid/get-version tmpid)         0))
      (is (= (uuid/to-string tmpid)       "00000000-0000-0000-0000-000000000000"))
      (is (= (uuid/to-urn-string tmpid)
             "urn:uuid:00000000-0000-0000-0000-000000000000"))
      (is (= (uuid/get-timestamp tmpid)      nil))
      (is (= (uuid/get-unix-time tmpid)      nil))))

  (testing "v1 uuid protocol..."
    (let [tmpid uuid/+namespace-x500+]
      (is (= (uuid/get-word-high tmpid)       7757371281853190609))
      (is (= (uuid/get-word-low tmpid)        -9172705715073830712))
      (is (= (uuid/null? tmpid)           false))
      (is (= (seq (uuid/to-byte-array tmpid))
             [107 -89 -72 20 -99 -83 17 -47 -128 -76 0 -64 79 -44 48 -56]))
      (is (= (uuid/get-version tmpid)         1))
      (is (= (uuid/to-string tmpid)     "6ba7b814-9dad-11d1-80b4-00c04fd430c8"))
      (is (= (uuid/to-urn-string tmpid)
             "urn:uuid:6ba7b814-9dad-11d1-80b4-00c04fd430c8"))
      (is (= (uuid/get-timestamp tmpid)      131059232331511828))
      (is (= (uuid/get-unix-time tmpid)      886630433151))))

  (testing "v3 uuid protocol..."
    (let [tmpid (java.util.UUID/fromString
                  "d9c53a66-fde2-3d04-b5ad-dce3848df07e")]
      (is (= (uuid/get-word-high tmpid)       -2754731383046652668))
      (is (= (uuid/get-word-low tmpid)        -5355381512134070146))
      (is (= (uuid/null? tmpid)           false))
      (is (= (seq (uuid/to-byte-array tmpid))
             [-39 -59 58 102 -3 -30 61 4 -75 -83 -36 -29 -124 -115 -16 126]))
      (is (= (uuid/get-version tmpid)         3))
      (is (= (uuid/to-string tmpid)       "d9c53a66-fde2-3d04-b5ad-dce3848df07e"))
      (is (= (uuid/to-urn-string tmpid)
             "urn:uuid:d9c53a66-fde2-3d04-b5ad-dce3848df07e"))
      (is (= (uuid/get-timestamp tmpid)       nil))
      (is (= (uuid/get-unix-time tmpid)       nil))))

  (testing "v4 uuid protocol..."
    (let [tmpid #uuid "3eb1e29a-4747-4a7d-8e40-94e245f57dc0"]
      (is (= (uuid/get-word-high tmpid)       4517641053478013565))
      (is (= (uuid/get-word-low tmpid)       -8196387622257066560))
      (is (= (uuid/null? tmpid)               false))
      (is (= (seq (uuid/to-byte-array tmpid))
             [62 -79 -30 -102 71 71 74 125 -114 64 -108 -30 69 -11 125 -64]))
      (is (= (uuid/get-version tmpid)         4))
      (is (= (uuid/to-string tmpid)       "3eb1e29a-4747-4a7d-8e40-94e245f57dc0"))
      (is (= (uuid/to-urn-string tmpid)
             "urn:uuid:3eb1e29a-4747-4a7d-8e40-94e245f57dc0"))
      (is (= (uuid/get-timestamp tmpid)       nil))
      (is (= (uuid/get-unix-time tmpid)       nil))))

  (testing "max uuid protocol..."
    (let [tmpid uuid/+max+]
      (is (= (uuid/get-word-high tmpid)       -1))
      (is (= (uuid/get-word-low tmpid)        -1))
      (is (= (uuid/null? tmpid)               false))
      (is (= (uuid/max? tmpid)                true))
      (is (= (seq (uuid/to-byte-array tmpid)) [-1 -1 -1 -1 -1 -1 -1 -1
                                               -1 -1 -1 -1 -1 -1 -1 -1]))
      (is (= (uuid/get-version tmpid)         0xf))
      (is (= (uuid/to-string tmpid)       "ffffffff-ffff-ffff-ffff-ffffffffffff"))
      (is (= (uuid/to-urn-string tmpid)
             "urn:uuid:ffffffff-ffff-ffff-ffff-ffffffffffff"))
      (is (= (uuid/get-timestamp tmpid)      nil))
      (is (= (uuid/get-unix-time tmpid)      nil))))

  (testing "v6 uuid protocol..."
    (let [tmpid #uuid "1ef3f06f-16db-6ff0-bb01-1b50e6f39e7f"]
      (is (= (uuid/get-word-high tmpid)       2230390600394043376))
      (is (= (uuid/get-word-low tmpid)        -4971662479354257793))
      (is (= (uuid/null? tmpid)               false))
      (is (= (uuid/max? tmpid)                false))
      (is (= (seq (uuid/to-byte-array tmpid)) [30  -13 -16 111 22  -37 111 -16
                                               -69 1   27  80  -26 -13 -98 127]))
      (is (= (uuid/get-version tmpid)         6))
      (is (= (uuid/to-string tmpid)       "1ef3f06f-16db-6ff0-bb01-1b50e6f39e7f"))
      (is (= (uuid/to-urn-string tmpid)
             "urn:uuid:1ef3f06f-16db-6ff0-bb01-1b50e6f39e7f"))
      (is (= (uuid/get-timestamp tmpid)      0x1ef3f06f16dbff0))
      (is (= (uuid/get-unix-time tmpid)      1720648452463))))

  (testing "v7 uuid protocol..."
    (let [tmpid #uuid "01909eae-4801-753a-bcd5-0889c34ac129"]
      (is (= (uuid/get-word-high tmpid)       112764462053815610))
      (is (= (uuid/get-word-low tmpid)        -4839952836759731927))
      (is (= (uuid/null? tmpid)               false))
      (is (= (uuid/max? tmpid)                false))
      (is (= (seq (uuid/to-byte-array tmpid)) [1   -112 -98 -82  72  1  117 58
                                               -68 -43  8   -119 -61 74 -63 41]))
      (is (= (uuid/get-version tmpid)         7))
      (is (= (uuid/to-string tmpid)       "01909eae-4801-753a-bcd5-0889c34ac129"))
      (is (= (uuid/to-urn-string tmpid)
             "urn:uuid:01909eae-4801-753a-bcd5-0889c34ac129"))
      (is (= (uuid/get-timestamp tmpid)      1720649140225))
      (is (= (uuid/get-unix-time tmpid)      0x01909eae4801))))

  (testing "v8 uuid protocol..."
    (let [tmpid #uuid "ffffffff-ffff-8fff-bfff-ffffffffffff"]
      (is (= (uuid/get-word-high tmpid)       -28673))
      (is (= (uuid/get-word-low tmpid)        -4611686018427387905))
      (is (= (uuid/null? tmpid)               false))
      (is (= (uuid/max? tmpid)                false))
      (is (= (seq (uuid/to-byte-array tmpid)) [-1  -1 -1 -1 -1 -1 -113 -1
                                               -65 -1 -1 -1 -1 -1 -1   -1]))
      (is (= (uuid/get-version tmpid)         8))
      (is (= (uuid/to-string tmpid)       "ffffffff-ffff-8fff-bfff-ffffffffffff"))
      (is (= (uuid/to-urn-string tmpid)
             "urn:uuid:ffffffff-ffff-8fff-bfff-ffffffffffff"))
      (is (= (uuid/get-timestamp tmpid)      nil))
      (is (= (uuid/get-unix-time tmpid)      nil)))))


(deftest check-predicates
  (testing "string predicates..."
    (is (uuid/uuid-string?       (uuid/to-string       (uuid/v4))))
    (is (uuid/uuid-urn-string?   (uuid/to-urn-string   (uuid/v4))))))


(deftest nil-test
  (testing "Calling certain functions/methods on nil returns nil"
    (testing "UUIDNameBytes"
      (is (thrown? IllegalArgumentException (uuid/as-byte-array nil))))

    (testing "UUIDable"
      (is (thrown? IllegalArgumentException (uuid/as-uuid nil)))
      (is (false? (uuid/uuidable? nil))))

    (testing "UUIDRfc9562"
      (is (false? (uuid/uuid? nil))))

    (is (false? (uuid/uuid-string? nil)))

    (is (false? (uuid/uuid-urn-string? nil)))))


(deftest byte-array-round-trip-test
  (testing "round-trip via byte-array"
    (let [u #uuid "4787199e-c0e2-4609-b5b8-284f2b7d117d"]
      (is (= u (uuid/as-uuid (uuid/as-byte-array u)))))))


(deftest uri-round-trip-test
  (testing "round-trip via URI"
    (let [u (uuid/v4)
          uri (uuid/to-uri u)]
      (is (instance? java.net.URI uri))
      (is (uuid/uuidable? uri))
      (is (= u (uuid/as-uuid uri))))))


(deftest string-conversion-test
  (testing "UUID from string conversion"
    (is (= #uuid "6ba7b810-9dad-11d1-80b4-00c04fd430c8"
           (uuid/as-uuid "6ba7b810-9dad-11d1-80b4-00c04fd430c8")))
    (is (= #uuid "6ba7b810-9dad-11d1-80b4-00c04fd430c8"
           (uuid/as-uuid "urn:uuid:6ba7b810-9dad-11d1-80b4-00c04fd430c8")))))


(deftest comparison-test
  (testing "UUID comparison operators"
    (let [u1 uuid/+null+
          u2 (uuid/v4)
          u3 uuid/+max+]
      (is (uuid/< u1 u2))
      (is (uuid/< u2 u3))
      (is (uuid/< u1 u3))
      (is (uuid/> u3 u2))
      (is (uuid/> u2 u1))
      (is (uuid/= u1 u1))
      (is (not (uuid/= u1 u2))))))


(deftest hex-string-test
  (testing "hex string conversion"
    (let [u #uuid "6ba7b810-9dad-11d1-80b4-00c04fd430c8"]
      (is (= "6ba7b8109dad11d180b400c04fd430c8" (uuid/to-hex-string u))))))
