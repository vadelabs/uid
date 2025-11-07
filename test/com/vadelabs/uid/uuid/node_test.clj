(ns com.vadelabs.uid.uuid.node-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.vadelabs.uid.uuid.node :refer [node-id +node-id+]]))

(deftest check-node-id
  (testing "existence and type of node id..."
    (is (= (node-id) (node-id)))
    (is (coll? (node-id)))
    (is (= 6 (count (node-id))))
    (is (every? number? (node-id)))
    (is (= 1 (bit-and 0x01 @+node-id+)))
    (is (instance? Long @+node-id+))))

(deftest check-node-id-deterministic
  (testing "node-id returns same value on multiple calls"
    (let [n1 (node-id)
          n2 (node-id)]
      (is (= n1 n2)))))

(deftest check-node-id-multicast-bit
  (testing "node-id has multicast bit set (least significant bit)"
    (is (odd? (last (node-id))))))
