package com.ubirch

/**
  * Object that contains configuration keys
  */
object ConfPaths {

  trait GenericConfPaths {
    final val NAME = "tokenSystem.name"
  }

  trait HttpServerConfPaths {
    final val PORT = "tokenSystem.server.port"
    final val SWAGGER_PATH = "tokenSystem.server.swaggerPath"
  }

  trait ExecutionContextConfPaths {
    final val THREAD_POOL_SIZE = "tokenSystem.executionContext.threadPoolSize"
  }

  trait CassandraClusterConfPaths {
    final val CONTACT_POINTS = "tokenSystem.cassandra.cluster.contactPoints"
    final val CONSISTENCY_LEVEL = "tokenSystem.cassandra.cluster.consistencyLevel"
    final val SERIAL_CONSISTENCY_LEVEL = "tokenSystem.cassandra.cluster.serialConsistencyLevel"
    final val WITH_SSL = "tokenSystem.cassandra.cluster.withSSL"
    final val USERNAME = "tokenSystem.cassandra.cluster.username"
    final val PASSWORD = "tokenSystem.cassandra.cluster.password"
    final val KEYSPACE = "tokenSystem.cassandra.cluster.keyspace"
    final val PREPARED_STATEMENT_CACHE_SIZE = "tokenSystem.cassandra.cluster.preparedStatementCacheSize"
  }

  trait PrometheusConfPaths {
    final val PORT = "tokenSystem.metrics.prometheus.port"
  }

  object GenericConfPaths extends GenericConfPaths
  object HttpServerConfPaths extends HttpServerConfPaths

}
