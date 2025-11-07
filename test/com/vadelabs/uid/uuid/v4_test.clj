(ns com.vadelabs.uid.uuid.v4-test
  "Random UUIDs tests"
  (:require
    [clojure.test :refer [deftest is testing]]
    [com.vadelabs.uid.uuid.interface :as uuid]))


(deftest check-v4-special-cases
  (testing "v4 special case correctness..."
    (is (= (uuid/v4  0  0)  #uuid "00000000-0000-4000-8000-000000000000"))
    (is (= (uuid/v4  0  1)  #uuid "00000000-0000-4000-8000-000000000001"))
    (is (= (uuid/v4  0 -1)  #uuid "00000000-0000-4000-bfff-ffffffffffff"))
    (is (= (uuid/v4 -1  0)  #uuid "ffffffff-ffff-4fff-8000-000000000000"))
    (is (= (uuid/v4 -1 -1)  #uuid "ffffffff-ffff-4fff-bfff-ffffffffffff"))))


(deftest check-v4-format
  (testing "v4 UUID has correct version and variant"
    (let [u (uuid/v4)]
      (is (= 4 (uuid/get-version u)))
      (is (= 2 (uuid/get-variant u))))))


(deftest check-v4-uniqueness
  (testing "v4 UUIDs are unique"
    (let [uuids (repeatedly 10000 uuid/v4)]
      (is (= (count uuids) (count (set uuids)))))))
