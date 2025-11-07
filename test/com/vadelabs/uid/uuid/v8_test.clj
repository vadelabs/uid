(ns com.vadelabs.uid.uuid.v8-test
  "Custom UUIDs tests"
  (:require
    [clojure.test :refer [deftest is testing]]
    [com.vadelabs.uid.uuid.interface :as uuid]))


(deftest check-v8-special-cases
  (testing "v8 custom UUID"
    (is (= (uuid/v8  0  0)  #uuid "00000000-0000-8000-8000-000000000000"))
    (is (= (uuid/v8  0  1)  #uuid "00000000-0000-8000-8000-000000000001"))
    (is (= (uuid/v8  0 -1)  #uuid "00000000-0000-8000-bfff-ffffffffffff"))
    (is (= (uuid/v8 -1  0)  #uuid "ffffffff-ffff-8fff-8000-000000000000"))
    (is (= (uuid/v8 -1 -1)  #uuid "ffffffff-ffff-8fff-bfff-ffffffffffff"))))


(deftest check-v8-format
  (testing "v8 UUID has correct version and variant"
    (let [u (uuid/v8 12345 67890)]
      (is (= 8 (uuid/get-version u)))
      (is (= 2 (uuid/get-variant u))))))
