(ns benchmark.id-generation
  "Comprehensive benchmark comparison of ID generation strategies.

  This benchmark compares all supported ID types:
  - UUID v0 (null UUID - constant)
  - UUID v1 (time-based with MAC address)
  - UUID v3 (name-based with MD5)
  - UUID v4 (random)
  - UUID v5 (name-based with SHA1)
  - UUID v6 (time-based, lexically sortable)
  - UUID v7 (Unix time-based, lexically sortable)
  - UUID v8 (custom UUID with user data)
  - UUID max (maximum UUID - constant)
  - SQUUID (sequential UUID - non-standard)
  - Flake (192-bit time-ordered IDs)

  Metrics:
  - Generation speed (throughput)
  - Memory usage (object size)
  - Sortability (time ordering)
  - String representation performance
  - Collision resistance"
  (:require
   [com.vadelabs.uid.interface :as uid]))


;; =============================================================================
;; Benchmark Utilities
;; =============================================================================

(defn warmup
  "Warmup JVM to reduce JIT compilation effects"
  [f n]
  (dotimes [_ n]
    (f)))


(defn time-ns
  "Execute function and return execution time in nanoseconds"
  [f]
  (let [start (System/nanoTime)]
    (f)
    (- (System/nanoTime) start)))


(defn bench
  "Run benchmark n times and return statistics"
  [label f n]
  (println (format "\n%s (n=%,d):" label n))

  ;; Warmup
  (print "  Warming up... ")
  (flush)
  (warmup f 1000)
  (println "done")

  ;; Benchmark
  (print "  Benchmarking... ")
  (flush)
  (let [times (vec (repeatedly n #(time-ns f)))
        sorted-times (sort times)
        total (reduce + times)
        mean (/ total n)
        median (nth sorted-times (quot n 2))
        p95 (nth sorted-times (int (* n 0.95)))
        p99 (nth sorted-times (int (* n 0.99)))
        min-time (first sorted-times)
        max-time (last sorted-times)]
    (println "done")
    (println (format "  Mean:       %,8.2f ns/op" (double mean)))
    (println (format "  Median:     %,8.2f ns/op" (double median)))
    (println (format "  P95:        %,8.2f ns/op" (double p95)))
    (println (format "  P99:        %,8.2f ns/op" (double p99)))
    (println (format "  Min:        %,8.2f ns/op" (double min-time)))
    (println (format "  Max:        %,8.2f ns/op" (double max-time)))
    (println (format "  Throughput: %,d ops/sec" (long (/ 1e9 mean))))
    {:label label
     :mean mean
     :median median
     :p95 p95
     :p99 p99
     :min min-time
     :max max-time
     :throughput (/ 1e9 mean)}))


(defn memory-size
  "Estimate memory size of an object"
  [obj]
  (let [baos (java.io.ByteArrayOutputStream.)
        oos (java.io.ObjectOutputStream. baos)]
    (.writeObject oos obj)
    (.flush oos)
    (.size baos)))


;; =============================================================================
;; Benchmark Constants
;; =============================================================================

;; For name-based UUIDs (v3, v5)
(def benchmark-namespace uid/+namespace-dns+)
(def benchmark-name "benchmark.vadelabs.com")

;; For v8 custom UUID (using valid signed long values)
(def v8-msb 0x0123456789ABCDEF)
(def v8-lsb 0x0EDCBA9876543210)


;; =============================================================================
;; Generation Benchmarks
;; =============================================================================

(defn bench-uuid-v0-generation [n]
  (bench "UUID v0 (null)" #(uid/v0) n))

(defn bench-uuid-v1-generation [n]
  (bench "UUID v1" #(uid/v1) n))

(defn bench-uuid-v3-generation [n]
  (bench "UUID v3" #(uid/v3 benchmark-namespace benchmark-name) n))

(defn bench-uuid-v4-generation [n]
  (bench "UUID v4" #(uid/v4) n))

(defn bench-uuid-v5-generation [n]
  (bench "UUID v5" #(uid/v5 benchmark-namespace benchmark-name) n))

(defn bench-uuid-v6-generation [n]
  (bench "UUID v6" #(uid/v6) n))

(defn bench-uuid-v7-generation [n]
  (bench "UUID v7" #(uid/v7) n))

(defn bench-uuid-v8-generation [n]
  (bench "UUID v8" #(uid/v8 v8-msb v8-lsb) n))

(defn bench-uuid-max-generation [n]
  (bench "UUID max" #(uid/max) n))

(defn bench-squuid-generation [n]
  (bench "SQUUID" #(uid/squuid) n))

(defn bench-flake-generation [n]
  (bench "Flake" #(uid/flake) n))


;; =============================================================================
;; String Conversion Benchmarks
;; =============================================================================

(defn bench-uuid-to-string [label uuid-fn n]
  (let [id (uuid-fn)]
    (bench (format "%s → String" label) #(str id) n)))

(defn bench-flake-to-string [n]
  (bench "Flake → String" #(uid/snowflake) n))


;; =============================================================================
;; Memory Usage Analysis
;; =============================================================================

(defn analyze-memory-usage
  []
  (println "\n=== Memory Usage Analysis ===")

  (let [v0-id (uid/v0)
        v1-id (uid/v1)
        v3-id (uid/v3 benchmark-namespace benchmark-name)
        v4-id (uid/v4)
        v5-id (uid/v5 benchmark-namespace benchmark-name)
        v6-id (uid/v6)
        v7-id (uid/v7)
        v8-id (uid/v8 v8-msb v8-lsb)
        max-id (uid/max)
        squuid-id (uid/squuid)
        flake-id (uid/flake)
        v0-str (str v0-id)
        v1-str (str v1-id)
        v3-str (str v3-id)
        v4-str (str v4-id)
        v5-str (str v5-id)
        v6-str (str v6-id)
        v7-str (str v7-id)
        v8-str (str v8-id)
        max-str (str max-id)
        squuid-str (str squuid-id)
        flake-str (uid/snowflake)]

    (println "\nObject sizes:")
    (println (format "  UUID v0:    %3d bytes" (memory-size v0-id)))
    (println (format "  UUID v1:    %3d bytes" (memory-size v1-id)))
    (println (format "  UUID v3:    %3d bytes" (memory-size v3-id)))
    (println (format "  UUID v4:    %3d bytes" (memory-size v4-id)))
    (println (format "  UUID v5:    %3d bytes" (memory-size v5-id)))
    (println (format "  UUID v6:    %3d bytes" (memory-size v6-id)))
    (println (format "  UUID v7:    %3d bytes" (memory-size v7-id)))
    (println (format "  UUID v8:    %3d bytes" (memory-size v8-id)))
    (println (format "  UUID max:   %3d bytes" (memory-size max-id)))
    (println (format "  SQUUID:     %3d bytes" (memory-size squuid-id)))
    (println (format "  Flake:      %3d bytes" (memory-size flake-id)))

    (println "\nString sizes:")
    (println (format "  UUID v0:    %3d bytes (%d chars)" (memory-size v0-str) (count v0-str)))
    (println (format "  UUID v1:    %3d bytes (%d chars)" (memory-size v1-str) (count v1-str)))
    (println (format "  UUID v3:    %3d bytes (%d chars)" (memory-size v3-str) (count v3-str)))
    (println (format "  UUID v4:    %3d bytes (%d chars)" (memory-size v4-str) (count v4-str)))
    (println (format "  UUID v5:    %3d bytes (%d chars)" (memory-size v5-str) (count v5-str)))
    (println (format "  UUID v6:    %3d bytes (%d chars)" (memory-size v6-str) (count v6-str)))
    (println (format "  UUID v7:    %3d bytes (%d chars)" (memory-size v7-str) (count v7-str)))
    (println (format "  UUID v8:    %3d bytes (%d chars)" (memory-size v8-str) (count v8-str)))
    (println (format "  UUID max:   %3d bytes (%d chars)" (memory-size max-str) (count max-str)))
    (println (format "  SQUUID:     %3d bytes (%d chars)" (memory-size squuid-str) (count squuid-str)))
    (println (format "  Flake:      %3d bytes (%d chars)" (memory-size flake-str) (count flake-str)))

    (println "\nExample representations:")
    (println (format "  UUID v0:    %s" v0-str))
    (println (format "  UUID v1:    %s" v1-str))
    (println (format "  UUID v3:    %s" v3-str))
    (println (format "  UUID v4:    %s" v4-str))
    (println (format "  UUID v5:    %s" v5-str))
    (println (format "  UUID v6:    %s" v6-str))
    (println (format "  UUID v7:    %s" v7-str))
    (println (format "  UUID v8:    %s" v8-str))
    (println (format "  UUID max:   %s" max-str))
    (println (format "  SQUUID:     %s" squuid-str))
    (println (format "  Flake:      %s" flake-str))))


;; =============================================================================
;; Sortability Analysis
;; =============================================================================

(defn analyze-sortability
  [n]
  (println (format "\n=== Sortability Analysis (n=%,d) ===" n))

  ;; Generate IDs with small delays
  (let [v1-ids (atom [])
        v3-ids (atom [])
        v4-ids (atom [])
        v5-ids (atom [])
        v6-ids (atom [])
        v7-ids (atom [])
        v8-ids (atom [])
        squuid-ids (atom [])
        flake-ids (atom [])]

    (dotimes [i n]
      (swap! v1-ids conj (uid/v1))
      (swap! v3-ids conj (uid/v3 benchmark-namespace (str "name-" i)))
      (swap! v4-ids conj (uid/v4))
      (swap! v5-ids conj (uid/v5 benchmark-namespace (str "name-" i)))
      (swap! v6-ids conj (uid/v6))
      (swap! v7-ids conj (uid/v7))
      (swap! v8-ids conj (uid/v8 (long i) (long i)))
      (swap! squuid-ids conj (uid/squuid))
      (swap! flake-ids conj (uid/flake))
      (Thread/sleep 0 1000)) ; 1 microsecond delay

    ;; Check if sorted
    (let [v1-sorted? (= @v1-ids (sort @v1-ids))
          v3-sorted? (= @v3-ids (sort @v3-ids))
          v4-sorted? (= @v4-ids (sort @v4-ids))
          v5-sorted? (= @v5-ids (sort @v5-ids))
          v6-sorted? (= @v6-ids (sort @v6-ids))
          v7-sorted? (= @v7-ids (sort @v7-ids))
          v8-sorted? (= @v8-ids (sort @v8-ids))
          squuid-sorted? (= @squuid-ids (sort @squuid-ids))
          flake-sorted? (= @flake-ids (sort @flake-ids))
          v1-str-sorted? (= (mapv str @v1-ids) (sort (mapv str @v1-ids)))
          v3-str-sorted? (= (mapv str @v3-ids) (sort (mapv str @v3-ids)))
          v4-str-sorted? (= (mapv str @v4-ids) (sort (mapv str @v4-ids)))
          v5-str-sorted? (= (mapv str @v5-ids) (sort (mapv str @v5-ids)))
          v6-str-sorted? (= (mapv str @v6-ids) (sort (mapv str @v6-ids)))
          v7-str-sorted? (= (mapv str @v7-ids) (sort (mapv str @v7-ids)))
          v8-str-sorted? (= (mapv str @v8-ids) (sort (mapv str @v8-ids)))
          squuid-str-sorted? (= (mapv str @squuid-ids) (sort (mapv str @squuid-ids)))
          flake-str-sorted? (= (mapv str @flake-ids) (sort (mapv str @flake-ids)))]

      (println "\nObject sorting (time-ordered):")
      (println (format "  UUID v1:    %s" (if v1-sorted? "✓ PASS" "✗ FAIL")))
      (println (format "  UUID v3:    %s (deterministic - expected to fail time order)" (if v3-sorted? "✓ PASS" "✗ FAIL")))
      (println (format "  UUID v4:    %s (random - expected to fail)" (if v4-sorted? "✓ PASS" "✗ FAIL")))
      (println (format "  UUID v5:    %s (deterministic - expected to fail time order)" (if v5-sorted? "✓ PASS" "✗ FAIL")))
      (println (format "  UUID v6:    %s" (if v6-sorted? "✓ PASS" "✗ FAIL")))
      (println (format "  UUID v7:    %s" (if v7-sorted? "✓ PASS" "✗ FAIL")))
      (println (format "  UUID v8:    %s (custom - expected to fail time order)" (if v8-sorted? "✓ PASS" "✗ FAIL")))
      (println (format "  SQUUID:     %s" (if squuid-sorted? "✓ PASS" "✗ FAIL")))
      (println (format "  Flake:      %s" (if flake-sorted? "✓ PASS" "✗ FAIL")))

      (println "\nString sorting (lexicographic):")
      (println (format "  UUID v1:    %s" (if v1-str-sorted? "✓ PASS" "✗ FAIL")))
      (println (format "  UUID v3:    %s (deterministic - expected to fail time order)" (if v3-str-sorted? "✓ PASS" "✗ FAIL")))
      (println (format "  UUID v4:    %s (random - expected to fail)" (if v4-str-sorted? "✓ PASS" "✗ FAIL")))
      (println (format "  UUID v5:    %s (deterministic - expected to fail time order)" (if v5-str-sorted? "✓ PASS" "✗ FAIL")))
      (println (format "  UUID v6:    %s" (if v6-str-sorted? "✓ PASS" "✗ FAIL")))
      (println (format "  UUID v7:    %s" (if v7-str-sorted? "✓ PASS" "✗ FAIL")))
      (println (format "  UUID v8:    %s (custom - expected to fail time order)" (if v8-str-sorted? "✓ PASS" "✗ FAIL")))
      (println (format "  SQUUID:     %s" (if squuid-str-sorted? "✓ PASS" "✗ FAIL")))
      (println (format "  Flake:      %s" (if flake-str-sorted? "✓ PASS" "✗ FAIL"))))))


;; =============================================================================
;; Collision Analysis
;; =============================================================================

(defn analyze-collisions
  [n]
  (println (format "\n=== Collision Analysis (n=%,d) ===" n))

  ;; Generate many IDs and check for duplicates
  ;; Note: v0 and max are constants, so we skip them
  ;; v3 and v5 are deterministic with same input, so we skip them
  (let [v1-ids (set (repeatedly n #(uid/v1)))
        v4-ids (set (repeatedly n #(uid/v4)))
        v6-ids (set (repeatedly n #(uid/v6)))
        v7-ids (set (repeatedly n #(uid/v7)))
        v8-ids (set (repeatedly n #(uid/v8 (long (rand-int 1000000)) (long (rand-int 1000000)))))
        squuid-ids (set (repeatedly n #(uid/squuid)))
        flake-ids (set (repeatedly n #(uid/flake)))
        v1-collisions (- n (count v1-ids))
        v4-collisions (- n (count v4-ids))
        v6-collisions (- n (count v6-ids))
        v7-collisions (- n (count v7-ids))
        v8-collisions (- n (count v8-ids))
        squuid-collisions (- n (count squuid-ids))
        flake-collisions (- n (count flake-ids))]

    (println (format "  UUID v1:    %,d/%,d unique (collisions: %d)"
                     (count v1-ids) n v1-collisions))
    (println (format "  UUID v4:    %,d/%,d unique (collisions: %d)"
                     (count v4-ids) n v4-collisions))
    (println (format "  UUID v6:    %,d/%,d unique (collisions: %d)"
                     (count v6-ids) n v6-collisions))
    (println (format "  UUID v7:    %,d/%,d unique (collisions: %d)"
                     (count v7-ids) n v7-collisions))
    (println (format "  UUID v8:    %,d/%,d unique (collisions: %d)"
                     (count v8-ids) n v8-collisions))
    (println (format "  SQUUID:     %,d/%,d unique (collisions: %d)"
                     (count squuid-ids) n squuid-collisions))
    (println (format "  Flake:      %,d/%,d unique (collisions: %d)"
                     (count flake-ids) n flake-collisions))

    (doseq [[label collisions] [["UUID v1" v1-collisions]
                                  ["UUID v4" v4-collisions]
                                  ["UUID v6" v6-collisions]
                                  ["UUID v7" v7-collisions]
                                  ["UUID v8" v8-collisions]
                                  ["SQUUID" squuid-collisions]
                                  ["Flake" flake-collisions]]]
      (when (pos? collisions)
        (println (format "  ⚠ Warning: %s collisions detected!" label))))))


;; =============================================================================
;; Comparison Tables
;; =============================================================================

(defn print-comparison-table
  [stats-list]
  (println "\n=== Performance Comparison Table ===")

  ;; Sort by mean latency (fastest first)
  (let [sorted-stats (sort-by :mean stats-list)
        fastest (:mean (first sorted-stats))]

    (println "\n┌─────────────────┬──────────────┬──────────────┬────────────────┬──────────────┐")
    (println "│ ID Type         │ Mean (ns)    │ P99 (ns)     │ Throughput     │ vs Fastest   │")
    (println "├─────────────────┼──────────────┼──────────────┼────────────────┼──────────────┤")

    (doseq [stats sorted-stats]
      (let [speedup (/ (:mean stats) fastest)]
        (println (format "│ %-15s │ %,12.2f │ %,12.2f │ %,14d │ %11.2fx │"
                         (:label stats)
                         (double (:mean stats))
                         (double (:p99 stats))
                         (long (:throughput stats))
                         (double speedup)))))

    (println "└─────────────────┴──────────────┴──────────────┴────────────────┴──────────────┘")))


(defn print-recommendations
  []
  (println "\n=== Use Case Recommendations ===")

  (println "\n**UUID v0** (Null UUID)")
  (println "  ✓ Pros: Constant value, extremely fast")
  (println "  ✗ Cons: Single fixed value (00000000-0000-0000-0000-000000000000)")
  (println "  → Use when: Representing null/empty UUID, placeholder value")

  (println "\n**UUID v1** (Time-based with MAC)")
  (println "  ✓ Pros: Good performance, time-ordered, includes node ID")
  (println "  ✗ Cons: Not lexically sortable, privacy concerns (MAC address)")
  (println "  → Use when: Legacy compatibility needed, node tracking important")

  (println "\n**UUID v3** (Name-based MD5)")
  (println "  ✓ Pros: Deterministic, good for deduplication")
  (println "  ✗ Cons: Not time-ordered, MD5 security concerns")
  (println "  → Use when: Reproducible IDs from names/namespaces, legacy systems")

  (println "\n**UUID v4** (Random)")
  (println "  ✓ Pros: Best performance, no coordination needed, privacy-friendly")
  (println "  ✗ Cons: Not time-ordered, not sortable")
  (println "  → Use when: Random IDs acceptable, no ordering requirements")

  (println "\n**UUID v5** (Name-based SHA1)")
  (println "  ✓ Pros: Deterministic, better security than v3")
  (println "  ✗ Cons: Not time-ordered, slower than v3")
  (println "  → Use when: Reproducible IDs from names, better security than v3")

  (println "\n**UUID v6** (Time-based, sortable)")
  (println "  ✓ Pros: Time-ordered, lexically sortable, improved v1")
  (println "  ✗ Cons: Moderate performance")
  (println "  → Use when: Time-ordering + sortability needed, standard compliance")

  (println "\n**UUID v7** (Unix time-based)")
  (println "  ✓ Pros: Time-ordered, lexically sortable, millisecond precision")
  (println "  ✗ Cons: Slightly slower than v6")
  (println "  → Use when: Modern standard compliance, database indexing")

  (println "\n**UUID v8** (Custom)")
  (println "  ✓ Pros: Maximum flexibility, embed custom data")
  (println "  ✗ Cons: Application-specific, requires careful design")
  (println "  → Use when: Need to embed custom data in UUID format")

  (println "\n**UUID max** (Maximum UUID)")
  (println "  ✓ Pros: Constant value, extremely fast")
  (println "  ✗ Cons: Single fixed value (ffffffff-ffff-ffff-ffff-ffffffffffff)")
  (println "  → Use when: Upper bound marker, range queries, testing")

  (println "\n**SQUUID** (Sequential UUID)")
  (println "  ✓ Pros: Time-ordered, good performance, database-friendly")
  (println "  ✗ Cons: Non-standard, not RFC9562 compliant")
  (println "  → Use when: Need time-ordered UUIDs with good DB performance")

  (println "\n**Flake** (192-bit nanosecond)")
  (println "  ✓ Pros: Fastest generation, nanosecond precision, high entropy")
  (println "  ✗ Cons: Slow string conversion, larger objects, non-standard")
  (println "  → Use when: Maximum throughput needed, rarely convert to strings"))


;; =============================================================================
;; Main Benchmark Suite
;; =============================================================================

(defn run-benchmarks
  "Run comprehensive benchmark suite"
  ([]
   (run-benchmarks 10000))
  ([n]
   (println "╔════════════════════════════════════════════════════════════════╗")
   (println "║         Comprehensive ID Generation Benchmark Suite           ║")
   (println "║  UUID v0-v8 | SQUUID | Flake                                  ║")
   (println "╚════════════════════════════════════════════════════════════════╝")

   ;; Generation benchmarks
   (println "\n## ID Generation Performance ##")
   (let [v0-stats (bench-uuid-v0-generation n)
         v1-stats (bench-uuid-v1-generation n)
         v3-stats (bench-uuid-v3-generation n)
         v4-stats (bench-uuid-v4-generation n)
         v5-stats (bench-uuid-v5-generation n)
         v6-stats (bench-uuid-v6-generation n)
         v7-stats (bench-uuid-v7-generation n)
         v8-stats (bench-uuid-v8-generation n)
         max-stats (bench-uuid-max-generation n)
         squuid-stats (bench-squuid-generation n)
         flake-stats (bench-flake-generation n)]

     ;; String conversion benchmarks
     (println "\n## String Conversion Performance ##")
     (bench-uuid-to-string "UUID v0" uid/v0 n)
     (bench-uuid-to-string "UUID v1" uid/v1 n)
     (bench-uuid-to-string "UUID v3" #(uid/v3 benchmark-namespace benchmark-name) n)
     (bench-uuid-to-string "UUID v4" uid/v4 n)
     (bench-uuid-to-string "UUID v5" #(uid/v5 benchmark-namespace benchmark-name) n)
     (bench-uuid-to-string "UUID v6" uid/v6 n)
     (bench-uuid-to-string "UUID v7" uid/v7 n)
     (bench-uuid-to-string "UUID v8" #(uid/v8 v8-msb v8-lsb) n)
     (bench-uuid-to-string "UUID max" uid/max n)
     (bench-uuid-to-string "SQUUID" uid/squuid n)
     (bench-flake-to-string n)

     ;; Memory analysis
     (analyze-memory-usage)

     ;; Sortability analysis
     (analyze-sortability 1000)

     ;; Collision analysis
     (analyze-collisions 100000)

     ;; Comparison table
     (print-comparison-table [v0-stats v1-stats v3-stats v4-stats v5-stats
                              v6-stats v7-stats v8-stats max-stats
                              squuid-stats flake-stats])

     ;; Recommendations
     (print-recommendations)

     (println "\n╔════════════════════════════════════════════════════════════════╗")
     (println "║                    Benchmark Complete                          ║")
     (println "╚════════════════════════════════════════════════════════════════╝\n"))))


(defn -main
  [& args]
  (let [n (if (seq args)
            (Integer/parseInt (first args))
            10000)]
    (run-benchmarks n)))


(comment
  ;; Run benchmark suite with default iterations (10,000)
  (run-benchmarks)

  ;; Run with custom iteration count
  (run-benchmarks 50000)

  ;; Run individual benchmarks
  (bench-uuid-v0-generation 10000)
  (bench-uuid-v1-generation 10000)
  (bench-uuid-v3-generation 10000)
  (bench-uuid-v4-generation 10000)
  (bench-uuid-v5-generation 10000)
  (bench-uuid-v6-generation 10000)
  (bench-uuid-v7-generation 10000)
  (bench-uuid-v8-generation 10000)
  (bench-uuid-max-generation 10000)
  (bench-squuid-generation 10000)
  (bench-flake-generation 10000)

  ;; Analyze specific aspects
  (analyze-memory-usage)
  (analyze-sortability 1000)
  (analyze-collisions 100000))
