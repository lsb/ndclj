(ns nd.htmlconversions-test
  (:require [clojure.test :refer :all]
  	    [clj-leveldb]
	    [clojure.java.jdbc]
	    [nd.db :as db]
	    [clojure.java.io :as io]
            [nd.htmlconversions :refer :all]))

(deftest backwards-compatibility
  (testing "Every Clojure passage should be rendered as it had rendered in Ruby."
    (let [sqldb (db/db-pool-of-n 1 (.getFile (io/resource "old-rb-db/nd.db")))
          htmldb (clj-leveldb/create-db (io/resource "old-rb-passages") {})
	  tsdb (org.custommonkey.xmlunit.TolerantSaxDocumentBuilder. (org.custommonkey.xmlunit.XMLUnit/getTestParser))
	  hdb (org.custommonkey.xmlunit.HTMLDocumentBuilder. tsdb)
	  ]
      (clojure.java.jdbc/execute! sqldb [(str "attach '" (.getPath (io/resource "old-rb-db/mde.db")) "' as m")] :transaction? false)
      (doall
        (map
          (fn [i]
	    (let [rbstr (String. (clj-leveldb/get htmldb i))
                  cljstr (String. (nd.htmlconversions/subpassageid-to-html sqldb (Integer. i)))
                  rbd (.parse hdb (.replaceAll rbstr "\r" " "))
                  cljd (.parse hdb (.replaceAll cljstr "\r" " "))
                  diff (org.custommonkey.xmlunit.Diff. rbd cljd)]
	      (if (not (.similar diff)) (println diff) (spit "/dev/stderr" "."))
	      (is (.similar diff))))
	  (map (fn [[k v]] (String. k)) (clj-leveldb/iterator htmldb))
        )))))

