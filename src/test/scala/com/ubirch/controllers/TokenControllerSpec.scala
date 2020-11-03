package com.ubirch.controllers

import java.util.UUID

import com.ubirch.models.{ TokenCreationData, TokenRow }
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

    "list OK" taggedAs Tag("orange") in {

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

      post("/v1/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        assert(jsonConverter.as[TokenCreationData](body).right.get.isInstanceOf[TokenCreationData])
      }

      get("/v1", headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        val res = jsonConverter.as[List[TokenRow]](body)
        assert(res.isRight)
        assert(res.right.get.size == 2)
      }

    }

    "delete OK" taggedAs Tag("apple") in {

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

      post("/v1/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        assert(jsonConverter.as[TokenCreationData](body).right.get.isInstanceOf[TokenCreationData])
      }

      var current: List[TokenRow] = Nil
      get("/v1", headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        val res = jsonConverter.as[List[TokenRow]](body)
        assert(res.isRight)
        assert(res.right.get.size == 2)
        current = res.right.get
      }

      val toDelete = current.headOption.map(_.id).map(_.toString)
      assert(toDelete.isDefined)
      delete("/v1/" + toDelete.get, headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        assert(body == """{"version":"1.0","status":"OK","message":"Token deleted"}""")
      }

      get("/v1", headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        val res = jsonConverter.as[List[TokenRow]](body)
        assert(res.isRight)
        assert(res.right.get.size == 1)
      }

    }

    "fail when no access token provided: create" taggedAs Tag("mandarina") in {

      val incomingBody = "{}"
      post("/v1/create", body = incomingBody) {
        status should equal(401)
        assert(body == """{"version":"1.0","status":"NOK","errorType":"AuthenticationError","errorMessage":"Unauthenticated"}""")
      }

    }

    "fail when no access token provided: list" taggedAs Tag("avocado") in {

      get("/v1") {
        status should equal(401)
        assert(body == """{"version":"1.0","status":"NOK","errorType":"AuthenticationError","errorMessage":"Unauthenticated"}""")
      }

    }

    "fail when no access token provided: delete" taggedAs Tag("strawberry") in {

      delete("/v1/" + UUID.randomUUID().toString) {
        status should equal(401)
        assert(body == """{"version":"1.0","status":"NOK","errorType":"AuthenticationError","errorMessage":"Unauthenticated"}""")
      }

    }

    "fail when invalid access token provided: create" taggedAs Tag("durian") in {

      val incomingBody = "{}"
      post("/v1/create", body = incomingBody, headers = Map("authorization" -> UUID.randomUUID().toString)) {
        status should equal(400)
        assert(body == """{"version":"1.0","status":"NOK","errorType":"AuthenticationError","errorMessage":"Invalid bearer token"}""")
      }

    }

    "fail when invalid access token provided: list" taggedAs Tag("cherry") in {

      get("/v1", headers = Map("authorization" -> UUID.randomUUID().toString)) {
        status should equal(400)
        assert(body == """{"version":"1.0","status":"NOK","errorType":"AuthenticationError","errorMessage":"Invalid bearer token"}""")
      }

    }

    "fail when invalid access token provided: delete" taggedAs Tag("blackberry") in {

      delete("/v1/" + UUID.randomUUID().toString, headers = Map("authorization" -> UUID.randomUUID().toString)) {
        status should equal(400)
        assert(body == """{"version":"1.0","status":"NOK","errorType":"AuthenticationError","errorMessage":"Invalid bearer token"}""")
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
