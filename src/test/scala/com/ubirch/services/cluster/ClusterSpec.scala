package com.ubirch.services.cluster

import com.github.nosan.embedded.cassandra.api.cql.CqlScript
import com.google.inject.Guice
import com.ubirch.{ Binder, EmbeddedCassandra, TestBase }

/**
  * Test for the cassandra cluster
  */
class ClusterSpec extends TestBase with EmbeddedCassandra {

  val cassandra = new CassandraTest

  lazy val serviceInjector = Guice.createInjector(new Binder())

  "Cluster and Cassandra Context" must {

    "be able to get proper instance and do query" in {

      val connectionService = serviceInjector.getInstance(classOf[ConnectionService])

      val db = connectionService.context

      val t = db.executeQuery("SELECT * FROM tokens").headOptionL.runToFuture
      assert(await(t).nonEmpty)
    }

    "be able to get proper instance and do query without recreating it" in {

      val connectionService = serviceInjector.getInstance(classOf[ConnectionService])

      val db = connectionService.context

      val t = db.executeQuery("SELECT * FROM tokens").headOptionL.runToFuture
      assert(await(t).nonEmpty)
    }

  }

  override protected def afterAll(): Unit = {

    val connectionService = serviceInjector.getInstance(classOf[ConnectionService])

    val db = connectionService.context

    db.close()

    cassandra.stop()
  }

  override protected def beforeAll(): Unit = {
    cassandra.start()

    (EmbeddedCassandra.creationScripts ++ List(
      CqlScript.ofString(
        "INSERT INTO token_system.tokens (owner_id, id, created_at, token_value) VALUES (963995ed-ce12-4ea5-89dc-b181701d1d7b, 63f64e20-ff46-49b6-abe3-cb9efda4afaf, '2020-10-30 13:01:54.202', 'eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJpc3MiOiIiLCJzdWIiOiIiLCJhdWQiOiIiLCJpYXQiOjE2MDQwNjI5MTQsImp0aSI6IjYzZjY0ZTIwLWZmNDYtNDliNi1hYmUzLWNiOWVmZGE0YWZhZiIsIm93bmVySWQiOiI5NjM5OTVlZC1jZTEyLTRlYTUtODlkYy1iMTgxNzAxZDFkN2IifQ._RBnk9-k13nVtWV-TTRGqxn6emyfdCgn-nwf9GvC7ogQwnuPzKSF5rKJfELBkuIHRpgygc8mslt5kJqC_cd_KQ');".stripMargin
      )
    )).foreach(x => x.forEachStatement(cassandra.connection.execute _))

  }
}
