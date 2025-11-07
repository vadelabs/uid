(ns com.vadelabs.uid.uuid.util
  (:import
    (java.util
      UUID)))


(defmacro returning
  "Compute a return value, then execute other forms for side effects.
  Like prog1 in common lisp, or a (do) that returns the first form."
  [value & forms]
  `(let [value# ~value]
     ~@forms
     value#))


(defn java6?
  []
  (neg? (compare (System/getProperty "java.version") "1.7")))


(defmacro compile-if
  "Evaluate `exp` and if it returns logical true and doesn't error, expand to
  `then` otherwise expand to `else`."
  [exp then else]
  (if (try (eval exp)
           (catch Throwable _ false))
    `(do ~then)
    `(do ~else)))


(defmacro with-timing
  "Same as clojure.core/time but returns a vector of a the result of
   the code and the milliseconds rather than printing a string. Runs
   the code in an implicit do."
  [& body]
  `(let [start# (System/nanoTime)  ret# ~(cons 'do body)]
     [ret# (/ (double (- (System/nanoTime) start#)) 1000000.0)]))


(defmacro wrap-fn
  [fn-name args & body]
  `(let [old-fn# (var-get (var ~fn-name))
         new-fn# (fn [& p#]
                   (let [~args p#]
                     (do ~@body)))
         wrapper# (fn [& params#]
                    (if (= ~(count args) (count params#))
                      (apply new-fn# params#)
                      (apply old-fn# params#)))]
     (alter-var-root (var ~fn-name) (constantly wrapper#))))


(defmacro with-temp-file
  [f-sym & body]
  `(let [prefix#  (.toString (UUID/randomUUID))
         postfix# (.toString (UUID/randomUUID))
         ~f-sym   (java.io.File/createTempFile prefix# postfix#)]
     (try
       (do ~@body)
       (finally
         (.delete ~f-sym)))))


(defn lines-of-file
  [^String file-name]
  (line-seq
    (java.io.BufferedReader.
      (java.io.InputStreamReader.
        (java.io.FileInputStream. file-name)))))
