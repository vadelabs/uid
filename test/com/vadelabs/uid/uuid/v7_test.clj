(ns com.vadelabs.uid.uuid.v7-test
  (:require
    [clojure.set]
    [clojure.test :refer [deftest is testing]]
    [com.vadelabs.uid.uuid.clock :as clock]
    [com.vadelabs.uid.uuid.interface :as uuid]))


(deftest check-v7-single-threaded
  (let [iterations 100000
        groups     5]
    (testing "single-thread v7 uuid uniqueness..."
      (dotimes [_ groups]
        (let [result (repeatedly iterations uuid/v7)]
          (is (= (count result) (count (set result)))))))))


(deftest check-v7-concurrency
  (doseq [concur (range 2 5)]
    (let [extent    10000
          agents    (map agent (repeat concur nil))
          working   (map #(send-off %
                                    (fn [_state]
                                      (repeatedly extent uuid/v7)))
                         agents)
          _         (apply await working)
          answers   (map deref working)]
      (testing (str "concurrent v7 uuid uniqueness (" concur " threads)...")
        (is (= (* concur extent)
               (count (apply clojure.set/union (map set answers))))))

      (testing (str "concurrent v7 monotonic increasing (" concur " threads)...")
        (is (every? identity
                    (map (partial apply uuid/<) answers)))))))


(deftest check-get-timestamp
  (dotimes [_ 1000]
    (let [t (first (clock/monotonic-unix-time-and-random-counter))]
      (with-redefs [clock/monotonic-unix-time-and-random-counter (constantly [t (rand-int 4095)])]
        (is (= t (uuid/get-timestamp (uuid/v7)))
            "Timestamp should be retrievable from v7 UUID")))))


(deftest check-v7-format
  (testing "v7 UUID has correct version and variant"
    (let [u (uuid/v7)]
      (is (= 7 (uuid/get-version u)))
      (is (= 2 (uuid/get-variant u))))))


(deftest check-v7-lexical-ordering
  (testing "v7 UUIDs are lexically ordered"
    (let [u1 (uuid/v7)
          _ (Thread/sleep 2)
          u2 (uuid/v7)]
      (is (uuid/< u1 u2)))))
