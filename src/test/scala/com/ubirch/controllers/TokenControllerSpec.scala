package com.ubirch.controllers

import com.ubirch.models.TokenCreationData
import com.ubirch.services.formats.JsonConverterService
import com.ubirch.services.jwt.PublicKeyPoolService
import com.ubirch.{ EmbeddedCassandra, _ }
import io.prometheus.client.CollectorRegistry
import org.scalatest.{ BeforeAndAfterEach, Tag }
import org.scalatra.test.scalatest.ScalatraWordSpec

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Test for the Key Controller
  */
class TokenControllerSpec
  extends ScalatraWordSpec
  with EmbeddedCassandra
  with BeforeAndAfterEach
  with ExecutionContextsTests
  with Awaits {

  private val cassandra = new CassandraTest

  private lazy val Injector = new InjectorHelperImpl() {}
  private val jsonConverter = Injector.get[JsonConverterService]

  "Token Manager" must {

    "create OK" taggedAs Tag("plum") in {

      val token = Injector.get[FakeToken]

      val incomingBody =
        """
          |{
          |  "ownerId":"963995ed-ce12-4ea5-89dc-b181701d1d7b",
          |  "issuer":"",
          |  "subject":"",
          |  "audience":"",
          |  "expiration":null,
          |  "notBefore":null,
          |  "issuedAt":null,
          |  "content":{
          |    "ownerId":"963995ed-ce12-4ea5-89dc-b181701d1d7b"
          |  }
          |}
          |""".stripMargin

      post("/v1/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        assert(jsonConverter.as[TokenCreationData](body).right.get.isInstanceOf[TokenCreationData])
      }

    }

    "not create when owner is not the same as in accessToken" taggedAs Tag("plum") in {

      val token = Injector.get[FakeToken]

      val incomingBody =
        """
          |{
          |  "ownerId":"863995ed-ce12-4ea5-89dc-b181701d1d7b",
          |  "issuer":"",
          |  "subject":"",
          |  "audience":"",
          |  "expiration":null,
          |  "notBefore":null,
          |  "issuedAt":null,
          |  "content":{
          |    "ownerId":"963995ed-ce12-4ea5-89dc-b181701d1d7b"
          |  }
          |}
          |""".stripMargin

      post("/v1/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        println(body)
        status should equal(400)

      }

    }

  }

  override protected def beforeEach(): Unit = {
    CollectorRegistry.defaultRegistry.clear()
    EmbeddedCassandra.truncateScript.forEachStatement(cassandra.connection.execute _)
  }

  protected override def afterAll(): Unit = {
    cassandra.stop()
    super.afterAll()
  }

  protected override def beforeAll(): Unit = {

    CollectorRegistry.defaultRegistry.clear()
    cassandra.startAndCreateDefaults()

    lazy val pool = Injector.get[PublicKeyPoolService]
    await(pool.init, 2 seconds)

    lazy val tokenController = Injector.get[TokenController]

    addServlet(tokenController, "/*")

    super.beforeAll()
  }
}
