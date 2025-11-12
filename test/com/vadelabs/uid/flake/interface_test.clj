(ns com.vadelabs.uid.flake.interface-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [com.vadelabs.uid.flake.core :as flake]))


(deftest basic-flake-generation-test
  (testing "Basic flake generation"
    (let [f1 (flake/flake)
          f2 (flake/flake)]
      (is (flake/flake? f1))
      (is (flake/flake? f2))
      (is (not= f1 f2))
      (is (< (compare f1 f2) 0) "Flakes should be monotonic"))))


(deftest snowflake-string-generation-test
  (testing "Snowflake string generation"
    (let [s1 (flake/flake-string)
          s2 (flake/flake-string)]
      (is (string? s1))
      (is (string? s2))
      (is (= 32 (count s1)))
      (is (= 32 (count s2)))
      (is (not= s1 s2))
      (is (< (compare s1 s2) 0) "String representations should preserve ordering"))))


(deftest flake-representation-test
  (testing "Flake representation as string and bytes"
    (is (= "--------------------------------" (str (flake/make-flake 0 0 0))))
    (is (= "zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz" (str (flake/make-flake -1 -1 -1))))

    (let [bytez (byte-array 24)
          _ (java.util.Arrays/fill bytez (byte 0))]
      (is (= (flake/make-flake 0 0 0) (flake/make-flake bytez))))

    (let [bytez (byte-array 24)
          _ (java.util.Arrays/fill bytez (byte -1))]
      (is (= (flake/make-flake -1 -1 -1) (flake/make-flake bytez))))

    (let [f (flake/flake)
          bytez (flake/flake-bytes f)]
      (is (= f (flake/make-flake bytez))))))


(deftest flake-parsing-test
  (testing "Flake parsing"
    (is (nil? (flake/from-string nil)))
    (is (nil? (flake/from-string "Invalid !@#$%^& String")))
    (is (nil? (flake/from-string "too-short")))
    (is (nil? (flake/from-string "way-too-long-string-that-exceeds-32-characters")))

    (let [f (flake/flake)
          f-str (str f)]
      (is (= f-str (str (flake/from-string f-str)))))))


(deftest flake-predicate-test
  (testing "flake? predicate"
    (is (false? (flake/flake? nil)))
    (is (false? (flake/flake? "foo")))
    (is (false? (flake/flake? 123)))
    (is (true? (flake/flake? (flake/flake))))))


(deftest flake-time-test
  (testing "Flake timestamp extraction"
    (let [f (flake/flake)
          timestamp (flake/timestamp f)]
      (is (number? timestamp))
      (is (pos? timestamp)))))


(deftest flake-hex-test
  (testing "Flake hex representation"
    (let [f (flake/flake)
          hex (flake/as-hex f)]
      (is (string? hex))
      (is (= 48 (count hex)))
      (is (re-matches #"[0-9a-f]{48}" hex)))))


(deftest homomorphic-representation-test
  (testing "Homomorphic representation property"
    (let [flakes (repeatedly 100 #(flake/flake))
          sorted-flakes (sort flakes)
          sorted-strings (sort (map str flakes))]
      (is (= (map str sorted-flakes) sorted-strings)
          "String representations should maintain same ordering as flakes"))))


(deftest monotonic-property-test
  (testing "Monotonic property with real flakes"
    (let [flakes (repeatedly 1000 #(flake/flake))]
      (is (= flakes (sort flakes))
          "Generated flakes should already be in sorted order"))))


(deftest monotonic-property-strings-test
  (testing "Monotonic property with string representations"
    (let [flake-strings (repeatedly 1000 #(str (flake/flake)))]
      (is (= flake-strings (sort flake-strings))
          "Generated flake strings should already be in sorted order"))))


(deftest round-trip-parsing-test
  (testing "Round-trip parsing property"
    (dotimes [_ 1000]
      (let [f (flake/flake)
            f-str (str f)
            parsed (flake/from-string f-str)]
        (is (= f parsed) "Parsing a flake string should return the original flake")))))


(deftest hex-monotonic-test
  (testing "Hex representation maintains monotonic property"
    (let [hex-strings (repeatedly 100 #(flake/as-hex (flake/flake)))]
      (is (= hex-strings (sort hex-strings))
          "Hex representations should maintain monotonic ordering"))))
