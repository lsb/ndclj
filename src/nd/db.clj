(ns nd.db)

(defn db-pool-of-n [n path]
  (Class/forName "org.sqlite.JDBC")
  (let [cpds (doto (com.jolbox.bonecp.BoneCPDataSource.)
       	       (.setJdbcUrl (str "jdbc:sqlite:" path))
	       (.setMinConnectionsPerPartition 1)
	       (.setMaxConnectionsPerPartition n)
	       (.setPartitionCount 1))]
    {:datasource cpds}))
