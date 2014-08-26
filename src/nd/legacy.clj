(ns nd.legacy)

(org.apache.xml.security.Init/init)
(def canon (org.apache.xml.security.c14n.Canonicalizer/getInstance org.apache.xml.security.c14n.Canonicalizer/ALGO_ID_C14N_OMIT_COMMENTS) )
(defn canonicalized [s] (String. (.canonicalize canon (.getBytes s))))

(defn normalize-nsbp-raquo [s] (clojure.string/replace (clojure.string/replace s "&nbsp;" " ") "&raquo;" "heyheyhey"))
(defn normalize-multilang-definitions [s] (clojure.string/replace s #"<span class=\"(german|french|italian|spanish)\">[^>]*</span>" ""))
(defn normalize-wrap-in-html [s] (str "<html>" s "</html>"))

(def fully-normal (comp normalize-wrap-in-html normalize-nsbp-raquo normalize-multilang-definitions))

(def normalized-canonicalized (comp canonicalized fully-normal))