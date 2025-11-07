(ns com.vadelabs.uid.uuid.v6-test
  "Time based v6 UUIDs tests"
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.set]
            [com.vadelabs.uid.uuid.interface :as uuid]
            [com.vadelabs.uid.uuid.clock :as clock]))

(deftest check-v6-single-threaded
  (let [iterations 100000
        groups     5]
    (testing "single-thread v6 uuid uniqueness..."
      (dotimes [_ groups]
        (let [result (repeatedly iterations uuid/v6)]
          (is (= (count result) (count (set result)))))))))

(deftest check-v6-concurrency
  (doseq [concur (range 2 5)]
    (let [extent    10000
          agents    (map agent (repeat concur nil))
          working   (map #(send-off %
                            (fn [_state]
                              (repeatedly extent uuid/v6)))
                      agents)
          _         (apply await working)
          answers   (map deref working)]
      (testing (str "concurrent v6 uuid uniqueness (" concur " threads)...")
        (is (= (* concur extent)
               (count (apply clojure.set/union (map set answers))))))
      (testing (str "concurrent v6 monotonic increasing (" concur " threads)...")
        (is (every? identity (map (partial apply uuid/<) answers)))))))

(deftest check-get-timestamp
  (dotimes [_ 1000]
    (let [t (clock/monotonic-time)]
      (with-redefs [clock/monotonic-time (constantly t)]
        (is (= t (uuid/get-timestamp (uuid/v6)))
            "Timestamp should be retrievable from v6 UUID")))))

(deftest check-v6-format
  (testing "v6 UUID has correct version and variant"
    (let [u (uuid/v6)]
      (is (= 6 (uuid/get-version u)))
      (is (= 2 (uuid/get-variant u))))))

(deftest check-v6-lexical-ordering
  (testing "v6 UUIDs are lexically ordered"
    (let [u1 (uuid/v6)
          _ (Thread/sleep 2)
          u2 (uuid/v6)]
      (is (uuid/< u1 u2)))))
