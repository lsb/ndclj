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
    	  htmldb (clj-leveldb/create-db (io/resource "old-rb-passages") {})]
      (doall
        (map
	  (fn [i]
	    (is (= (nd.legacy/normalized-canonicalized (String. (clj-leveldb/get htmldb i))) (nd.legacy/normalized-canonicalized (nd.htmlconversions/subpassageid-to-html sqldb (Integer. i))))))
	  (map (fn [[k v]] (String. k)) (clj-leveldb/iterator htmldb)))))))

