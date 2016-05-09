(defproject nd "0.2.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main nd.core
  :aot :all
  :profiles {:test {:resource-paths ["resources-test"]}}
  :dependencies [[org.clojure/clojure "1.8.0"]
  		 [org.clojure/java.jdbc "0.3.3"]
		 [org.xerial/sqlite-jdbc "3.8.11.2"]
		 [hiccup "1.0.5"]
		 [net.sf.jtidy/jtidy "r938"]
		 [com.jolbox/bonecp "0.7.1.RELEASE"]
		 [org.slf4j/slf4j-log4j12 "1.6.6"]
		 [org.apache.santuario/xmlsec "2.0.0"]
		 [xmlunit/xmlunit "1.5"]
                 ])
