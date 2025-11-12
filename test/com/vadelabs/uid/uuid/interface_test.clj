(ns com.vadelabs.uid.uuid.interface-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [com.vadelabs.uid.uuid.core :as uuid])
  (:import
    (java.util
      UUID)))


(deftest null-uuid-test
  (testing "v0/null UUID generation"
    (let [u (uuid/null)]
      (is (instance? UUID u))
      (is (uuid/null? u))
      (is (= u uuid/+null+))
      (is (= "00000000-0000-0000-0000-000000000000" (str u))))))


(deftest max-uuid-test
  (testing "max UUID generation"
    (let [u (uuid/max)]
      (is (instance? UUID u))
      (is (uuid/max? u))
      (is (= u uuid/+max+))
      (is (= "ffffffff-ffff-ffff-ffff-ffffffffffff" (str u))))))


(deftest v1-uuid-test
  (testing "v1 UUID generation"
    (let [u (uuid/v1)]
      (is (instance? UUID u))
      (is (uuid/uuid? u))
      (is (= 1 (uuid/get-version u)))
      (is (= 2 (uuid/get-variant u)))
      (is (some? (uuid/get-timestamp u)))
      (is (some? (uuid/get-instant u)))
      (is (some? (uuid/get-unix-time u))))))


(deftest v4-uuid-test
  (testing "v4 UUID generation"
    (let [u (uuid/v4)]
      (is (instance? UUID u))
      (is (uuid/uuid? u))
      (is (= 4 (uuid/get-version u)))
      (is (= 2 (uuid/get-variant u)))))

  (testing "v4 UUID generation with explicit bits"
    (let [u (uuid/v4 0 0)]
      (is (instance? UUID u))
      (is (= 4 (uuid/get-version u)))
      (is (= 2 (uuid/get-variant u)))
      (is (= "00000000-0000-4000-8000-000000000000" (str u))))))


(deftest v6-uuid-test
  (testing "v6 UUID generation"
    (let [u (uuid/v6)]
      (is (instance? UUID u))
      (is (uuid/uuid? u))
      (is (= 6 (uuid/get-version u)))
      (is (= 2 (uuid/get-variant u)))
      (is (some? (uuid/get-timestamp u)))
      (is (some? (uuid/get-instant u))))))


(deftest v7-uuid-test
  (testing "v7 UUID generation"
    (let [u (uuid/v7)]
      (is (instance? UUID u))
      (is (uuid/uuid? u))
      (is (= 7 (uuid/get-version u)))
      (is (= 2 (uuid/get-variant u)))
      (is (some? (uuid/get-timestamp u)))
      (is (some? (uuid/get-instant u))))))


(deftest v3-uuid-test
  (testing "v3 UUID generation"
    (let [u1 (uuid/v3 uuid/+namespace-dns+ "example.com")
          u2 (uuid/v3 uuid/+namespace-dns+ "example.com")]
      (is (instance? UUID u1))
      (is (= 3 (uuid/get-version u1)))
      (is (= 2 (uuid/get-variant u1)))
      (is (uuid/= u1 u2) "Same input should produce same v3 UUID"))))


(deftest v5-uuid-test
  (testing "v5 UUID generation"
    (let [u1 (uuid/v5 uuid/+namespace-dns+ "example.com")
          u2 (uuid/v5 uuid/+namespace-dns+ "example.com")]
      (is (instance? UUID u1))
      (is (= 5 (uuid/get-version u1)))
      (is (= 2 (uuid/get-variant u1)))
      (is (uuid/= u1 u2) "Same input should produce same v5 UUID"))))


(deftest squuid-test
  (testing "SQUUID generation"
    (let [u (uuid/squuid)]
      (is (instance? UUID u))
      (is (uuid/uuid? u))
      (is (= 4 (uuid/get-version u))))))


(deftest uuid-conversion-test
  (testing "UUID string conversion"
    (let [u (uuid/v4)
          s (uuid/to-string u)]
      (is (string? s))
      (is (uuid/uuid-string? s))
      (is (uuid/uuidable? s))
      (is (uuid/= u (uuid/as-uuid s)))))

  (testing "UUID hex string conversion"
    (let [u (uuid/v4)
          h (uuid/to-hex-string u)]
      (is (string? h))
      (is (= 32 (count h)))))

  (testing "UUID URN conversion"
    (let [u (uuid/v4)
          urn (uuid/to-urn-string u)]
      (is (string? urn))
      (is (.startsWith urn "urn:uuid:"))
      (is (uuid/uuid-urn-string? urn))))

  (testing "UUID byte array conversion"
    (let [u (uuid/v4)
          ba (uuid/to-byte-array u)]
      (is (bytes? ba))
      (is (= 16 (count ba)))
      (is (uuid/= u (uuid/as-uuid ba))))))


(deftest uuid-comparison-test
  (testing "UUID equality"
    (let [u1 (uuid/v4)
          u2 (uuid/v4)]
      (is (uuid/= u1 u1))
      (is (not (uuid/= u1 u2)))))

  (testing "UUID ordering"
    (let [u1 (uuid/null)
          u2 (uuid/v4)]
      (is (uuid/< u1 u2))
      (is (uuid/> u2 u1))
      (is (not (uuid/< u2 u1))))))


(deftest monotonic-time-test
  (testing "Monotonic time is always increasing"
    (let [t1 (uuid/monotonic-time)
          t2 (uuid/monotonic-time)
          t3 (uuid/monotonic-time)]
      (is (< t1 t2))
      (is (< t2 t3)))))


(deftest v6-lexical-ordering-test
  (testing "v6 UUIDs are lexically sortable"
    (let [u1 (uuid/v6)
          _ (Thread/sleep 2)
          u2 (uuid/v6)]
      (is (uuid/< u1 u2) "Later v6 UUID should be greater"))))


(deftest v7-lexical-ordering-test
  (testing "v7 UUIDs are lexically sortable"
    (let [u1 (uuid/v7)
          _ (Thread/sleep 2)
          u2 (uuid/v7)]
      (is (uuid/< u1 u2) "Later v7 UUID should be greater"))))
