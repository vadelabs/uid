(ns com.vadelabs.uid.uuid.v1-test
  "Time based UUIDs tests"
  (:require
    [clojure.set]
    [clojure.test :refer [deftest is testing]]
    [com.vadelabs.uid.uuid.clock :as clock]
    [com.vadelabs.uid.uuid.interface :as uuid]))


(deftest check-v1-single-threaded
  (let [iterations 100000
        groups     5]
    (testing "single-thread v1 uuid uniqueness..."
      (dotimes [_ groups]
        (let [result (repeatedly iterations uuid/v1)]
          (is (= (count result) (count (set result)))))))))


(deftest check-v1-concurrency
  (doseq [concur (range 2 5)]
    (let [extent    10000
          agents    (map agent (repeat concur nil))
          working   (map #(send-off %
                                    (fn [_state]
                                      (repeatedly extent uuid/v1)))
                         agents)
          _         (apply await working)
          answers   (map deref working)]
      (testing (str "concurrent v1 uuid uniqueness (" concur " threads)...")
        (is (= (* concur extent)
               (count (apply clojure.set/union (map set answers))))))
      (testing (str "concurrent v1 monotonic increasing (" concur " threads)...")
        (is (every? identity
                    (map #(apply < (map uuid/get-timestamp %)) answers)))))))


(deftest check-get-timestamp
  (let [t (clock/monotonic-time)]
    (with-redefs [clock/monotonic-time (constantly t)]
      (is (= t (uuid/get-timestamp (uuid/v1)))
          "Timestamp should be retrievable from v1 UUID"))))


(deftest check-v1-format
  (testing "v1 UUID has correct version and variant"
    (let [u (uuid/v1)]
      (is (= 1 (uuid/get-version u)))
      (is (= 2 (uuid/get-variant u))))))
