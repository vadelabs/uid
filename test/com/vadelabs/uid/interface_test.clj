(ns com.vadelabs.uid.interface-test
  "Tests for the curated public API interface"
  (:require
    [clojure.test :refer [deftest is testing]]
    [com.vadelabs.uid.interface :as uid])
  (:import
    (java.util UUID Date)))


;; =============================================================================
;; UUID Generation Tests
;; =============================================================================

(deftest v4-generation-test
  (testing "v4 random UUID generation"
    (let [u1 (uid/v4)
          u2 (uid/v4)]
      (is (instance? UUID u1))
      (is (instance? UUID u2))
      (is (not= u1 u2) "Different v4 UUIDs should be unique")
      (is (= 4 (.version u1))))))


(deftest v7-generation-test
  (testing "v7 time-based UUID generation"
    (let [u1 (uid/v7)
          u2 (uid/v7)]
      (is (instance? UUID u1))
      (is (instance? UUID u2))
      (is (not= u1 u2))
      (is (= 7 (.version u1)))
      (is (uid/< u1 u2) "v7 UUIDs should be time-ordered"))))


(deftest v1-generation-test
  (testing "v1 time-based UUID generation"
    (let [u (uid/v1)]
      (is (instance? UUID u))
      (is (= 1 (.version u))))))


(deftest v6-generation-test
  (testing "v6 time-based UUID generation"
    (let [u1 (uid/v6)
          u2 (uid/v6)]
      (is (instance? UUID u1))
      (is (= 6 (.version u1)))
      (is (uid/< u1 u2) "v6 UUIDs should be time-ordered"))))


(deftest v5-generation-test
  (testing "v5 name-based UUID generation"
    (let [u1 (uid/v5 uid/+namespace-dns+ "example.com")
          u2 (uid/v5 uid/+namespace-dns+ "example.com")
          u3 (uid/v5 uid/+namespace-dns+ "different.com")]
      (is (instance? UUID u1))
      (is (= 5 (.version u1)))
      (is (uid/= u1 u2) "Same inputs should produce same v5 UUID")
      (is (not (uid/= u1 u3)) "Different inputs should produce different v5 UUIDs"))))


(deftest v3-generation-test
  (testing "v3 name-based UUID generation"
    (let [u1 (uid/v3 uid/+namespace-dns+ "example.com")
          u2 (uid/v3 uid/+namespace-dns+ "example.com")]
      (is (instance? UUID u1))
      (is (= 3 (.version u1)))
      (is (uid/= u1 u2) "Same inputs should produce same v3 UUID"))))


;; =============================================================================
;; UUID Constants Tests
;; =============================================================================

(deftest namespace-constants-test
  (testing "Standard namespace UUIDs are available"
    (is (instance? UUID uid/+namespace-dns+))
    (is (instance? UUID uid/+namespace-url+))
    (is (instance? UUID uid/+namespace-oid+))
    (is (instance? UUID uid/+namespace-x500+))
    (is (= uid/+namespace-dns+ #uuid "6ba7b810-9dad-11d1-80b4-00c04fd430c8"))
    (is (= uid/+namespace-url+ #uuid "6ba7b811-9dad-11d1-80b4-00c04fd430c8"))))


;; =============================================================================
;; UUID Core Operations Tests
;; =============================================================================

(deftest uuid?-test
  (testing "uuid? predicate"
    (is (uid/uuid? (uid/v4)))
    (is (uid/uuid? #uuid "550e8400-e29b-41d4-a716-446655440000"))
    (is (not (uid/uuid? "not-a-uuid")))
    (is (not (uid/uuid? nil)))
    (is (not (uid/uuid? 123)))))


(deftest as-uuid-test
  (testing "as-uuid coercion"
    (let [uuid-str "550e8400-e29b-41d4-a716-446655440000"
          urn-str "urn:uuid:550e8400-e29b-41d4-a716-446655440000"
          expected #uuid "550e8400-e29b-41d4-a716-446655440000"]
      (is (= expected (uid/as-uuid uuid-str)))
      (is (= expected (uid/as-uuid urn-str)))
      (is (= expected (uid/as-uuid expected))))))


(deftest uuid-equality-test
  (testing "UUID equality comparison"
    (let [u1 (uid/v4)
          u2 (uid/v4)
          u3 (uid/v5 uid/+namespace-dns+ "test")
          u4 (uid/v5 uid/+namespace-dns+ "test")]
      (is (uid/= u1 u1))
      (is (not (uid/= u1 u2)))
      (is (uid/= u3 u4))
      (is (uid/= u1 u1 u1))
      (is (not (uid/= u1 u2 u1))))))


(deftest uuid-ordering-test
  (testing "UUID ordering comparison"
    (let [u1 (uid/v7)
          u2 (uid/v7)
          u3 (uid/v7)]
      (is (uid/< u1 u2))
      (is (uid/> u2 u1))
      (is (uid/< u1 u2 u3))
      (is (uid/> u3 u2 u1)))))


;; =============================================================================
;; UUID Time Extraction Tests
;; =============================================================================

(deftest get-instant-test
  (testing "Extract instant from time-based UUIDs"
    (let [u7 (uid/v7)
          instant (uid/get-instant u7)]
      (is (instance? Date instant))
      (is (pos? (.getTime instant))))

    (testing "Returns nil for non-time-based UUIDs"
      (is (nil? (uid/get-instant (uid/v4)))))))


(deftest get-unix-time-test
  (testing "Extract unix time from time-based UUIDs"
    (let [u7 (uid/v7)
          unix-time (uid/get-unix-time u7)]
      (is (number? unix-time))
      (is (pos? unix-time))
      (is (< (Math/abs (- unix-time (System/currentTimeMillis))) 1000)
          "Timestamp should be within 1 second of now"))

    (testing "Returns nil for non-time-based UUIDs"
      (is (nil? (uid/get-unix-time (uid/v4)))))))


;; =============================================================================
;; Flake Generation Tests
;; =============================================================================

(deftest flake-generation-test
  (testing "Flake generation"
    (let [f1 (uid/flake)
          f2 (uid/flake)]
      (is (uid/flake? f1))
      (is (uid/flake? f2))
      (is (not= f1 f2))
      (is (< (compare f1 f2) 0) "Flakes should be monotonically increasing"))))


(deftest snowflake-generation-test
  (testing "Snowflake string generation"
    (let [s1 (uid/snowflake)
          s2 (uid/snowflake)]
      (is (string? s1))
      (is (string? s2))
      (is (= 32 (count s1)) "Snowflake strings should be 32 characters")
      (is (= 32 (count s2)))
      (is (not= s1 s2))
      (is (< (compare s1 s2) 0) "Snowflake strings should preserve ordering"))))


(deftest flake?-test
  (testing "flake? predicate"
    (is (uid/flake? (uid/flake)))
    (is (not (uid/flake? "not-a-flake")))
    (is (not (uid/flake? nil)))
    (is (not (uid/flake? (uid/v4))))))


(deftest flake-time-test
  (testing "Extract timestamp from Flake"
    (let [f (uid/flake)
          ts (uid/flake-time f)]
      (is (number? ts))
      (is (pos? ts))
      (is (> ts (* (System/currentTimeMillis) 1000000))
          "Timestamp should be in nanoseconds"))))


(deftest parse-flake-test
  (testing "Parse Flake from string"
    (let [f (uid/flake)
          f-str (str f)
          parsed (uid/parse-flake f-str)]
      (is (= f parsed) "Parsed Flake should equal original"))

    (testing "Returns nil for invalid strings"
      (is (nil? (uid/parse-flake "invalid")))
      (is (nil? (uid/parse-flake "too-short")))
      (is (nil? (uid/parse-flake nil))))))


;; =============================================================================
;; Integration Tests
;; =============================================================================

(deftest uuid-flake-interop-test
  (testing "UUIDs and Flakes are distinct types"
    (let [u (uid/v4)
          f (uid/flake)]
      (is (uid/uuid? u))
      (is (not (uid/uuid? f)))
      (is (uid/flake? f))
      (is (not (uid/flake? u))))))


(deftest api-completeness-test
  (testing "All documented functions are available"
    ;; UUID generation
    (is (fn? uid/v1))
    (is (fn? uid/v3))
    (is (fn? uid/v4))
    (is (fn? uid/v5))
    (is (fn? uid/v6))
    (is (fn? uid/v7))

    ;; UUID operations
    (is (fn? uid/uuid?))
    (is (fn? uid/as-uuid))
    (is (fn? uid/=))
    (is (fn? uid/<))
    (is (fn? uid/>))
    (is (fn? uid/get-instant))
    (is (fn? uid/get-unix-time))

    ;; Flake operations
    (is (fn? uid/flake))
    (is (fn? uid/snowflake))
    (is (fn? uid/flake?))
    (is (fn? uid/flake-time))
    (is (fn? uid/parse-flake))))
