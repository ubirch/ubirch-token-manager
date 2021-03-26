package com.ubirch.controllers

import java.security.PublicKey
import java.util.UUID

import com.ubirch.models.Good
import com.ubirch.services.formats.JsonConverterService
import com.ubirch.services.jwt.{ PublicKeyPoolService, TokenDecodingService }
import com.ubirch.{ EmbeddedCassandra, _ }
import io.prometheus.client.CollectorRegistry
import org.jose4j.jwk.PublicJsonWebKey
import org.scalatest.{ BeforeAndAfterEach, Tag }
import org.scalatra.test.scalatest.ScalatraWordSpec

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Test for the Key Controller
  */
class TokenControllerV1Spec
  extends ScalatraWordSpec
  with EmbeddedCassandra
  with BeforeAndAfterEach
  with ExecutionContextsTests
  with Awaits {

  private val cassandra = new CassandraTest

  private lazy val Injector = new InjectorHelperImpl() {}
  private val jsonConverter = Injector.get[JsonConverterService]

  "Token Manager -Generic Tokens-" must {

    "create OK" taggedAs Tag("plum") in {

      val token = Injector.get[FakeTokenCreator].admin

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
        assert(jsonConverter.as[Good](body).right.get.isInstanceOf[Good])
      }

    }

    "create OK and verified" taggedAs Tag("avocado") in {

      get("/v1/jwk") {
        status should equal(200)
        assert(body == """{"version":"1.0","ok":true,"data":{"kty":"EC","x":"Lgn8c96LBnxMOCkujWg-06uu8iDJuKa4WTWgVTWROac","y":"Dxey52VDUYoRP7qEhj22BguwIk_EUQTKCsioJ5sNdEo","crv":"P-256"}}""".stripMargin)
      }

      val token = Injector.get[FakeTokenCreator]
      val tokenVerificationService = Injector.get[TokenDecodingService]

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

      post("/v1/create", body = incomingBody, headers = Map("authorization" -> token.admin.prepare)) {
        status should equal(200)
        val good = jsonConverter.as[Good](body).right.get
        assert(jsonConverter.as[Good](body).right.get.isInstanceOf[Good])
        val token = good.data.asInstanceOf[Map[String, String]]("token")
        val key = PublicJsonWebKey.Factory.newPublicJwk("""{"kty":"EC","x":"Lgn8c96LBnxMOCkujWg-06uu8iDJuKa4WTWgVTWROac","y":"Dxey52VDUYoRP7qEhj22BguwIk_EUQTKCsioJ5sNdEo","crv":"P-256"}""").getKey
        val claims = tokenVerificationService.decodeAndVerify(token, key.asInstanceOf[PublicKey])
        assert(claims.isSuccess)
      }

    }

    "create not OK for not Admins" taggedAs Tag("plum") in {

      val token = Injector.get[FakeTokenCreator].user

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
        status should equal(403)
        assert(body == """{"version":"1.0","ok":false,"errorType":"AuthenticationError","errorMessage":"Forbidden"}""")
      }

    }

    "not create when owner is not the same as in accessToken" taggedAs Tag("plums") in {

      val token = Injector.get[FakeTokenCreator].admin

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
        status should equal(400)
        assert(body == """{"version":"1.0","ok":false,"errorType":"TokenCreationError","errorMessage":"Error creating token"}""")
      }

    }

    "list OK" taggedAs Tag("orange") in {

      val token = Injector.get[FakeTokenCreator].admin

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
        assert(jsonConverter.as[Good](body).right.get.isInstanceOf[Good])
      }

      post("/v1/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        assert(jsonConverter.as[Good](body).right.get.isInstanceOf[Good])
      }

      get("/v1", headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        val res = jsonConverter.as[Good](body)
        assert(res.isRight)
        val data = res.right.get.data.asInstanceOf[List[Map[String, String]]]
        assert(data.size == 2)
      }

    }

    "delete OK" taggedAs Tag("apple") in {

      val token = Injector.get[FakeTokenCreator].admin

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
        assert(jsonConverter.as[Good](body).right.get.isInstanceOf[Good])
      }

      post("/v1/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        assert(jsonConverter.as[Good](body).right.get.isInstanceOf[Good])
      }

      var current: List[Map[String, String]] = Nil
      get("/v1", headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        val res = jsonConverter.as[Good](body)
        assert(res.isRight)
        val data = res.right.get.data.asInstanceOf[List[Map[String, String]]]
        assert(data.size == 2)
        current = data
      }

      val toDelete = current.headOption.flatMap(_.find(_._1 == "id")).map(_._2)
      assert(toDelete.isDefined)
      delete("/v1/" + toDelete.get, headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        assert(body == """{"version":"1.0","ok":true,"data":"Token deleted"}""")
      }

      get("/v1", headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        val res = jsonConverter.as[Good](body)
        assert(res.isRight)
        val data = res.right.get.data.asInstanceOf[List[Map[String, String]]]
        assert(data.size == 1)
      }

    }

    "fail when no access token provided: create" taggedAs Tag("mandarina") in {

      val incomingBody = "{}"
      post("/v1/create", body = incomingBody) {
        status should equal(401)
        assert(body == """{"version":"1.0","ok":false,"errorType":"AuthenticationError","errorMessage":"Unauthenticated"}""")
      }

    }

    "fail when no access token provided: list" taggedAs Tag("avocado") in {

      get("/v1") {
        status should equal(401)
        assert(body == """{"version":"1.0","ok":false,"errorType":"AuthenticationError","errorMessage":"Unauthenticated"}""")
      }

    }

    "fail when no access token provided: delete" taggedAs Tag("strawberry") in {

      delete("/v1/" + UUID.randomUUID().toString) {
        status should equal(401)
        assert(body == """{"version":"1.0","ok":false,"errorType":"AuthenticationError","errorMessage":"Unauthenticated"}""")
      }

    }

    "fail when invalid access token provided: create" taggedAs Tag("durian") in {

      val incomingBody = "{}"
      post("/v1/create", body = incomingBody, headers = Map("authorization" -> UUID.randomUUID().toString)) {
        status should equal(400)
        assert(body == """{"version":"1.0","ok":false,"errorType":"AuthenticationError","errorMessage":"Invalid bearer token"}""")
      }

    }

    "fail when invalid access token provided: list" taggedAs Tag("cherry") in {

      get("/v1", headers = Map("authorization" -> UUID.randomUUID().toString)) {
        status should equal(400)
        assert(body == """{"version":"1.0","ok":false,"errorType":"AuthenticationError","errorMessage":"Invalid bearer token"}""")
      }

    }

    "fail when invalid access token provided: delete" taggedAs Tag("blackberry") in {

      delete("/v1/" + UUID.randomUUID().toString, headers = Map("authorization" -> UUID.randomUUID().toString)) {
        status should equal(400)
        assert(body == """{"version":"1.0","ok":false,"errorType":"AuthenticationError","errorMessage":"Invalid bearer token"}""")
      }

    }

    "get JWK OK" taggedAs Tag("macadamia") in {

      get("/v1/jwk") {
        status should equal(200)
        assert(body == """{"version":"1.0","ok":true,"data":{"kty":"EC","x":"Lgn8c96LBnxMOCkujWg-06uu8iDJuKa4WTWgVTWROac","y":"Dxey52VDUYoRP7qEhj22BguwIk_EUQTKCsioJ5sNdEo","crv":"P-256"}}""".stripMargin)
      }

    }

  }

  override protected def beforeEach(): Unit = {
    CollectorRegistry.defaultRegistry.clear()
    EmbeddedCassandra.truncateScript.forEachStatement { x => val _ = cassandra.connection.execute(x) }
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

    lazy val tokenController = Injector.get[TokenControllerV1]

    addServlet(tokenController, "/v1")

    super.beforeAll()
  }
}
