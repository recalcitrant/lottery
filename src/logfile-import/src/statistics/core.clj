(ns statistics.core
  (:gen-class)
  (require [clojure.string :as str]
           [yesql.core :refer [defqueries]])
  (import java.util.regex.Pattern
          (java.text ParseException SimpleDateFormat)))

(defqueries "sql/statements.sql")

(defmacro doseq-indexed
  "loops over a set of values, binding index-sym to the 0-based index of each value"
  ([[val-sym values index-sym] & code]
   `(loop [vals# (seq ~values)
           ~index-sym (long 0)]
      (if vals#
        (let [~val-sym (first vals#)]
          ~@code
          (recur (next vals#) (inc ~index-sym)))
        nil))))

(defn -main [& file]
  (let [p (Pattern/compile "^([\\d.]+) (\\S+) (\\S+) \\[([\\w:/]+\\s[+\\-]\\d{4})\\] \"(.+?)\" (\\d{3}) (\\d+) \"([^\"]+)\" \"([^\"]+)\"")
        url-start "/client/drawing/"
        overview "GET"
        query "POST"
        num-fields 9
        valid-blz-len 8]

    (def db
      {:subprotocol "mysql"
       :subname     "//localhost:3306/lottery"
       :user        "lottery"
       :password    "lottery"})

    (def branch-codes (into #{} (map #(get % :code) (get-branch-codes db))))

    (doseq-indexed [i (range 1 (Integer/parseInt (first (next file)))) outer]
                   (with-open [rdr (clojure.java.io/reader (str (first file) "." i))]
                     (doseq-indexed [line (line-seq rdr) inner]
                                    (println (str "outer : " outer))
                                    (println (str "inner : " inner))
                                    (let [matcher (.matcher p line)]
                                      (when (and (.contains line url-start) (or (.contains line overview) (.contains line query)))
                                        (if-not (or (.matches matcher) (= (num-fields) (.groupCount matcher)))
                                          (throw (RuntimeException.))
                                          (let [request (str/split (.group matcher 5) #" ")
                                                type (nth request 0 "")
                                                type-normalized (cond
                                                                  (.startsWith type overview) 0
                                                                  (.startsWith type query) 1
                                                                  :else -1)
                                                url (nth request 1 "")
                                                date (.group matcher 4)
                                                blz (try
                                                      (.substring url (inc (.lastIndexOf url "/")))
                                                      (catch Exception e ""))]
                                            (if (and (= valid-blz-len (.length blz)) (not= type-normalized -1))
                                              (try
                                                (let [blz-normalized (Integer/valueOf blz)
                                                      date-normalized (-> "d/MMM/yyyy:H:m:s X"
                                                                          SimpleDateFormat.
                                                                          (.parse date))]
;(println type-normalized)
;(println blz-normalized)
;(println date-normalized)
                                                  (when (contains? branch-codes blz-normalized)
                                                    (create-log-entry! db type-normalized blz-normalized date-normalized)))
                                                (catch NumberFormatException ne (println ne))
                                                (catch ParseException pe (println pe)))))))))))))
