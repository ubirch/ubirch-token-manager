package com.ubirch

import com.github.nosan.embedded.cassandra.EmbeddedCassandraFactory
import com.github.nosan.embedded.cassandra.api.Cassandra
import com.github.nosan.embedded.cassandra.api.connection.{ CassandraConnection, DefaultCassandraConnectionFactory }
import com.github.nosan.embedded.cassandra.api.cql.CqlScript
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._

/**
  * Tool for embedding cassandra
  */
trait EmbeddedCassandra {

  class CassandraTest extends LazyLogging {

    @volatile var cassandra: Cassandra = _
    @volatile var cassandraConnectionFactory: DefaultCassandraConnectionFactory = _
    @volatile var connection: CassandraConnection = _

    def start(): Unit = {
      val factory: EmbeddedCassandraFactory = new EmbeddedCassandraFactory()
      factory.getJvmOptions.addAll(List("-Xms500m", "-Xmx1000m").asJava)
      cassandra = factory.create()
      cassandra.start()
      cassandraConnectionFactory = new DefaultCassandraConnectionFactory
      connection = cassandraConnectionFactory.create(cassandra)

    }

    def stop(): Unit = {
      if (connection != null) try connection.close()
      catch {
        case ex: Throwable =>
          logger.error("CassandraConnection '" + connection + "' is not closed", ex)
      }
      cassandra.stop()
      if (cassandra != null) cassandra.stop()
    }

    def startAndCreateDefaults(scripts: Seq[CqlScript] = EmbeddedCassandra.creationScripts): Unit = {
      start()
      scripts.foreach(x => x.forEachStatement(connection.execute _))
    }

  }

}

object EmbeddedCassandra {

  def truncateScript: CqlScript = ???

  def creationScripts: Seq[CqlScript] = ???
}
