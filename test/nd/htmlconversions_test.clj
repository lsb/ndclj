(ns nd.htmlconversions-test
  (:require [clojure.test :refer :all]
	    [clojure.java.jdbc]
	    [nd.db :as db]
	    [clojure.java.io :as io]
            [nd.htmlconversions :refer :all]))

(deftest backwards-compatibility
  (testing "Every Clojure passage should be rendered as it had rendered in Ruby."
    (let [sqldb (db/db-pool-of-n 32 (.getFile (io/resource "old-rb-db/nd.db")))
          htmldb (db/db-pool-of-n 32 (.getFile (io/resource "old-rb-passages.db")))]
      (doall
        (pmap
          (fn [i]
	    (let [rbstr (:html (first (clojure.java.jdbc/query htmldb ["select html from htmls where id = ?" i])))
                  cljstr (String. (nd.htmlconversions/subpassageid-to-html sqldb (Integer. i)))
                  tsdb1 (org.custommonkey.xmlunit.TolerantSaxDocumentBuilder. (org.custommonkey.xmlunit.XMLUnit/getTestParser))
                  hdb1 (org.custommonkey.xmlunit.HTMLDocumentBuilder. tsdb1)
                  tsdb2 (org.custommonkey.xmlunit.TolerantSaxDocumentBuilder. (org.custommonkey.xmlunit.XMLUnit/getTestParser))
                  hdb2 (org.custommonkey.xmlunit.HTMLDocumentBuilder. tsdb2)
                  rbd (.parse hdb1 (.replaceAll rbstr "\r" " "))
                  cljd (.parse hdb2 (.replaceAll cljstr "\r" " "))
                  diff (org.custommonkey.xmlunit.Diff. rbd cljd)
                  similar (.similar diff)]
	      (spit "/dev/stderr" (if similar "." diff))
	      (is similar)))
	  (map :id (clojure.java.jdbc/query htmldb ["select id from htmls"])))))))
