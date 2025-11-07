(ns com.vadelabs.uid.uuid.clock-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.set]
            [com.vadelabs.uid.uuid.clock :refer [monotonic-time monotonic-unix-time-and-random-counter]]))

(deftest check-single-threaded
  (let [iterations 100000
        groups     5
        check      #(mapv (fn [_] (%)) (range iterations))]
    (testing "monotonic-time..."
      (dotimes [_ groups]
        (let [result   (check monotonic-time)]
          (is (= (count result) (count (set result)))))))
    (testing "monotonic-unix-time-and-random-counter..."
      (dotimes [_ groups]
        (let [result   (check monotonic-unix-time-and-random-counter)]
          (is (= (count result) (count (set result)))))))))

(deftest check-multi-threaded-monotonic-time
  (doseq [concur (range 2 5)]
    (let [extent    10000
          agents    (mapv agent (repeat concur nil))
          working   (mapv #(send-off %
                            (fn [_state]
                              (repeatedly extent monotonic-time)))
                      agents)
          _         (apply await working)
          answers   (mapv deref working)]
      (testing (str "concurrent timestamp uniqueness (" concur " threads)...")
        (is (= (* concur extent)
               (count (apply clojure.set/union (map set answers))))))
      (testing (str "concurrent monotonic increasing (" concur " threads)...")
        (is (every? identity
                    (map #(apply < %) answers)))))))

(deftest check-multi-threaded-monotonic-unix-time-and-random-counter
  (doseq [concur (range 2 5)]
    (let [extent  10000
          agents  (mapv agent (repeat concur nil))
          working (mapv #(send-off %
                           (fn [_state]
                             (repeatedly extent
                                         monotonic-unix-time-and-random-counter)))
                         agents)
          _       (apply await working)
          answers (mapv deref working)]
      (testing (str "concurrent timestamp uniqueness (" concur " threads)...")
        (is (=
              (* concur extent)
              (count (apply clojure.set/union (map set answers))))))
      (testing (str "concurrent monotonic increasing (" concur " threads)...")
        (doseq [answer answers]
          (let [[t cnt] (first answer)]
            (loop [t   t
                   cnt cnt
                   more (rest answer)]
              (when-let [[next-t next-cnt] (first more)]
                (cond
                  (< next-t t)
                  (is false "time must be increasing")

                  (and (= next-t t) (<= next-cnt cnt))
                  (is false "counter must be increasing")

                  :else
                  (recur next-t next-cnt (rest more)))))))))))

(deftest check-monotonic-time-always-increasing
  (testing "monotonic-time always returns increasing values"
    (dotimes [_ 1000]
      (let [t1 (monotonic-time)
            t2 (monotonic-time)]
        (is (< t1 t2))))))

(deftest check-monotonic-unix-time-format
  (testing "monotonic-unix-time-and-random-counter returns [time counter]"
    (let [[t cnt] (monotonic-unix-time-and-random-counter)]
      (is (integer? t))
      (is (integer? cnt))
      (is (pos? t))
      (is (>= cnt 0))
      (is (<= cnt 0xfff)))))
