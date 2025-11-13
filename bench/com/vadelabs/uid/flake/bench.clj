(ns com.vadelabs.uid.flake.bench
  "Performance benchmarks for Flake ID generation and operations.

  Run with: clojure -M:bench -m com.vadelabs.uid.flake.bench

  Verifies performance claims from implementation:
  - Generation: ~300ns (no locks, pure CPU)
  - Encoding: ~500ns (custom algorithm)
  - Parsing: ~600ns (with validation)"
  (:require
    [com.vadelabs.uid.flake.core :as flake]
    [com.vadelabs.uid.flake.impl :as impl]
    [criterium.core :refer [quick-bench bench]]))


(defn -main
  "Run all benchmarks and print results."
  [& args]
  (println "\n=== Flake Performance Benchmarks ===\n")

  ;; Benchmark 1: Flake generation
  (println "1. Flake Generation (target: ~300ns)")
  (println "   Creating new Flake with timestamp and random components:")
  (quick-bench (flake/flake))

  ;; Benchmark 2: String encoding
  (println "\n2. Flake String Encoding (target: ~500ns)")
  (println "   Converting Flake to URL-safe base64 string:")
  (let [f (flake/flake)]
    (quick-bench (impl/flake->string f)))

  ;; Benchmark 3: Flake generation + encoding (combined)
  (println "\n3. Flake Generation + String Encoding (target: ~800ns)")
  (println "   Complete operation from generation to string:")
  (quick-bench (flake/flake-string))

  ;; Benchmark 4: String parsing
  (println "\n4. String Parsing (target: ~600ns)")
  (println "   Parsing valid Flake from string representation:")
  (let [flake-str (str (flake/flake))]
    (quick-bench (flake/from-string flake-str)))

  ;; Benchmark 5: Hex encoding
  (println "\n5. Hex Encoding")
  (println "   Converting Flake to hexadecimal representation:")
  (let [f (flake/flake)]
    (quick-bench (flake/as-hex f)))

  ;; Benchmark 6: Byte array conversion
  (println "\n6. Byte Array Conversion")
  (println "   Converting Flake to 24-byte array:")
  (let [f (flake/flake)]
    (quick-bench (flake/as-bytes f)))

  ;; Benchmark 7: Byte array parsing
  (println "\n7. Byte Array Parsing")
  (println "   Creating Flake from 24-byte array:")
  (let [bytes (flake/as-bytes (flake/flake))]
    (quick-bench (flake/make-flake bytes)))

  ;; Benchmark 8: Timestamp extraction
  (println "\n8. Timestamp Extraction")
  (println "   Reading timestamp from Flake:")
  (let [f (flake/flake)]
    (quick-bench (flake/timestamp f)))

  ;; Benchmark 9: Flake comparison
  (println "\n9. Flake Comparison")
  (println "   Comparing two Flakes for ordering:")
  (let [f1 (flake/flake)
        f2 (flake/flake)]
    (quick-bench (compare f1 f2)))

  ;; Benchmark 10: Round-trip (generation -> string -> parse)
  (println "\n10. Round-trip: Generate -> String -> Parse")
  (println "    Complete cycle for serialization/deserialization:")
  (quick-bench
    (let [f (flake/flake)
          s (str f)
          parsed (flake/from-string s)]
      parsed))

  ;; Benchmark 11: Concurrent generation (multi-threaded)
  (println "\n11. Concurrent Generation (4 threads)")
  (println "    Testing ThreadLocal RNG isolation:")
  (quick-bench
    (let [futures (repeatedly 4 #(future (flake/flake)))]
      (doall (map deref futures))))

  (println "\n=== Benchmarks Complete ===\n"))


(defn run-detailed-benchmarks
  "Run detailed benchmarks with more iterations and statistics.
  Use this for thorough performance analysis."
  []
  (println "\n=== Detailed Flake Performance Analysis ===\n")

  (println "1. Detailed Flake Generation Analysis")
  (bench (flake/flake))

  (println "\n2. Detailed String Encoding Analysis")
  (let [f (flake/flake)]
    (bench (impl/flake->string f)))

  (println "\n3. Detailed String Parsing Analysis")
  (let [flake-str (str (flake/flake))]
    (bench (flake/from-string flake-str)))

  (println "\n=== Detailed Analysis Complete ===\n"))


(defn benchmark-at-scale
  "Benchmark operations at scale to detect performance degradation."
  []
  (println "\n=== Scale Benchmarks ===\n")

  (println "Generating 1,000 Flakes:")
  (quick-bench
    (dotimes [_ 1000]
      (flake/flake)))

  (println "\nGenerating 10,000 Flakes:")
  (quick-bench
    (dotimes [_ 10000]
      (flake/flake)))

  (println "\nGenerating 100,000 Flakes:")
  (quick-bench
    (dotimes [_ 100000]
      (flake/flake)))

  (println "\n=== Scale Benchmarks Complete ===\n"))


(defn compare-with-alternatives
  "Compare Flake performance with alternative approaches."
  []
  (println "\n=== Comparative Benchmarks ===\n")

  (println "1. Flake generation:")
  (quick-bench (flake/flake))

  (println "\n2. java.util.UUID/randomUUID:")
  (quick-bench (java.util.UUID/randomUUID))

  (println "\n3. (str (java.util.UUID/randomUUID)):")
  (quick-bench (str (java.util.UUID/randomUUID)))

  (println "\n4. Flake string generation:")
  (quick-bench (flake/flake-string))

  (println "\n=== Comparison Complete ===\n"))


(comment
  ;; Run quick benchmarks
  (-main)

  ;; Run detailed benchmarks with more iterations
  (run-detailed-benchmarks)

  ;; Test performance at scale
  (benchmark-at-scale)

  ;; Compare with alternatives
  (compare-with-alternatives)
  )
