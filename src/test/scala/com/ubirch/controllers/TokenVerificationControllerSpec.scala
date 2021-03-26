package com.ubirch.controllers

import java.security.PublicKey
import java.util.UUID

import com.ubirch.models.Good
import com.ubirch.services.formats.JsonConverterService
import com.ubirch.services.jwt.{ PublicKeyPoolService, TokenVerificationService }
import com.ubirch.{ EmbeddedCassandra, _ }
import io.prometheus.client.CollectorRegistry
import org.jose4j.jwk.PublicJsonWebKey
import org.json4s.JsonAST.{ JArray, JField, JObject, JString }
import org.scalatest.{ BeforeAndAfterEach, Tag }
import org.scalatra.test.scalatest.ScalatraWordSpec

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Test for the Key Controller
  */
class TokenVerificationControllerSpec
  extends ScalatraWordSpec
  with EmbeddedCassandra
  with BeforeAndAfterEach
  with ExecutionContextsTests
  with Awaits {

  private val cassandra = new CassandraTest

  private lazy val Injector = new InjectorHelperImpl() {}
  private val jsonConverter = Injector.get[JsonConverterService]
  private val tokenDecodingService = Injector.get[TokenVerificationService]

  "Token Manager -Verification Tokens-" must {

    "create OK with *" taggedAs Tag("plum") in {

      val token = Injector.get[FakeTokenCreator].user

      val incomingBody =
        """
          |{
          |  "tenantId":"963995ed-ce12-4ea5-89dc-b181701d1d7b",
          |  "purpose":"King Dude - Concert",
          |  "targetIdentities": "*",
          |  "expiration": 2233738785,
          |  "notBefore":null
          |}
          |""".stripMargin

      post("/v1/verification/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(200)

        val b = jsonConverter.as[Good](body).right.get
        val data = b.data.asInstanceOf[Map[String, Any]]

        data.get("token") match {
          case Some(token: String) =>

            val key = PublicJsonWebKey.Factory.newPublicJwk("""{"kty":"EC","x":"Lgn8c96LBnxMOCkujWg-06uu8iDJuKa4WTWgVTWROac","y":"Dxey52VDUYoRP7qEhj22BguwIk_EUQTKCsioJ5sNdEo","crv":"P-256"}""").getKey
            tokenDecodingService.decodeAndVerify(token, key.asInstanceOf[PublicKey]) match {
              case None => fail(new Exception("Failed"))
              case Some(value) =>

                def complete(completeWith: Boolean = false)(list: List[Boolean]): List[Boolean] = list match {
                  case Nil => List(completeWith)
                  case _ => list
                }

                assert(value.children.size == 11)

                val issOK = complete()(for {
                  JObject(child) <- value
                  JField("iss", JString("https://token.dev.ubirch.com")) <- child
                } yield true)

                val subOK = complete()(for {
                  JObject(child) <- value
                  JField("sub", JString("963995ed-ce12-4ea5-89dc-b181701d1d7b")) <- child
                } yield true)

                val audOK = complete()(for {
                  JObject(child) <- value
                  JField("aud", JString("https://verify.dev.ubirch.com")) <- child
                } yield true)

                val tidOK = complete()(for {
                  JObject(child) <- value
                  JField("target_identities", JString("*")) <- child
                } yield true)

                val ordOK = complete()(for {
                  JObject(child) <- value
                  JField("origin_domains", JArray(tid)) <- child
                } yield tid.isEmpty)

                val purposeOk = complete()(for {
                  JObject(child) <- value
                  JField("purpose", JString("King Dude - Concert")) <- child
                } yield true)

                val roleOk = complete()(for {
                  JObject(child) <- value
                  JField("role", JString("verifier")) <- child
                } yield true)

                val scopesOk = complete()(for {
                  JObject(child) <- value
                  JField("scope", JString("ver")) <- child
                } yield true)

                val check = issOK ++ audOK ++ subOK ++ tidOK ++ ordOK ++ purposeOk ++ roleOk ++ scopesOk

                assert(check.forall(b => b) && check.nonEmpty)

            }

          case _ => fail("No Token Found")
        }

        assert(jsonConverter.as[Good](body).right.get.isInstanceOf[Good])
      }

    }

    "create OK with list of domains" taggedAs Tag("pomegranate") in {

      val token = Injector.get[FakeTokenCreator].user

      val incomingBody =
        """
          |{
          |  "tenantId":"963995ed-ce12-4ea5-89dc-b181701d1d7b",
          |  "purpose":"King Dude - Concert",
          |  "targetIdentities": "*",
          |  "expiration": 2233738785,
          |  "notBefore":null,
          |  "originDomains": ["https://meet.google.com", "https://simple.wikipedia.org"]
          |}
          |""".stripMargin

      post("/v1/verification/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        assert(jsonConverter.as[Good](body).right.get.isInstanceOf[Good])
      }

    }

    "not create when schema is wrong" taggedAs Tag("pomegranate") in {

      val token = Injector.get[FakeTokenCreator].user

      val incomingBody =
        """
          |{
          |  "tenantId":"963995ed-ce12-4ea5-89dc-b181701d1d7b",
          |  "purpose":"King Dude - Concert",
          |  "targetIdentities": "*",
          |  "expiration": 2233738785,
          |  "notBefore":null,
          |  "originDomains": ["ftp://meet.google.com", "https://simple.wikipedia.org"]
          |}
          |""".stripMargin

      post("/v1/verification/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(400)
        assert(body == """{"version":"1.0","ok":false,"errorType":"TokenCreationError","errorMessage":"Error creating token"}""")
      }

    }

    "create OK" taggedAs Tag("plum") in {

      val token = Injector.get[FakeTokenCreator].user

      val incomingBody =
        """
          |{
          |  "tenantId":"963995ed-ce12-4ea5-89dc-b181701d1d7b",
          |  "purpose":"King Dude - Concert",
          |  "targetIdentities":["840b7e21-03e9-4de7-bb31-0b9524f3b500"],
          |  "expiration": 2233738785,
          |  "notBefore":null
          |}
          |""".stripMargin

      post("/v1/verification/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(200)

        val b = jsonConverter.as[Good](body).right.get
        val data = b.data.asInstanceOf[Map[String, Any]]

        data.get("token") match {
          case Some(token: String) =>

            val key = PublicJsonWebKey.Factory.newPublicJwk("""{"kty":"EC","x":"Lgn8c96LBnxMOCkujWg-06uu8iDJuKa4WTWgVTWROac","y":"Dxey52VDUYoRP7qEhj22BguwIk_EUQTKCsioJ5sNdEo","crv":"P-256"}""").getKey
            tokenDecodingService.decodeAndVerify(token, key.asInstanceOf[PublicKey]) match {
              case None => fail(new Exception("Failed"))
              case Some(value) =>

                def complete(completeWith: Boolean = false)(list: List[Boolean]): List[Boolean] = list match {
                  case Nil => List(completeWith)
                  case _ => list
                }

                assert(value.children.size == 11)

                val issOK = complete()(for {
                  JObject(child) <- value
                  JField("iss", JString("https://token.dev.ubirch.com")) <- child
                } yield true)

                val subOK = complete()(for {
                  JObject(child) <- value
                  JField("sub", JString("963995ed-ce12-4ea5-89dc-b181701d1d7b")) <- child
                } yield true)

                val audOK = complete()(for {
                  JObject(child) <- value
                  JField("aud", JString("https://verify.dev.ubirch.com")) <- child
                } yield true)

                val tidOK = complete()(for {
                  JObject(child) <- value
                  JField("target_identities", JArray(tid)) <- child
                  JString("840b7e21-03e9-4de7-bb31-0b9524f3b500") <- tid
                } yield true)

                val ordOK = complete()(for {
                  JObject(child) <- value
                  JField("origin_domains", JArray(tid)) <- child
                } yield tid.isEmpty)

                val purposeOk = complete()(for {
                  JObject(child) <- value
                  JField("purpose", JString("King Dude - Concert")) <- child
                } yield true)

                val roleOk = complete()(for {
                  JObject(child) <- value
                  JField("role", JString("verifier")) <- child
                } yield true)

                val scopesOk = complete()(for {
                  JObject(child) <- value
                  JField("scope", JString("ver")) <- child
                } yield true)

                val check = issOK ++ audOK ++ subOK ++ tidOK ++ ordOK ++ purposeOk ++ roleOk ++ scopesOk

                assert(check.forall(b => b) && check.nonEmpty)

            }

          case _ => fail("No Token Found")
        }

        assert(jsonConverter.as[Good](body).right.get.isInstanceOf[Good])
      }

    }

    "not create when owner is not the same as in accessToken" taggedAs Tag("plums") in {

      val token = Injector.get[FakeTokenCreator].user

      val incomingBody =
        """
          |{
          |  "tenantId":"982995ed-ce12-4ea5-89dc-b181701d1d7b",
          |  "purpose":"King Dude - Concert",
          |  "targetIdentities":["840b7e21-03e9-4de7-bb31-0b9524f3b500"],
          |  "expiration": 2233738785,
          |  "notBefore":null
          |}
          |""".stripMargin

      post("/v1/verification/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(400)
        assert(body == """{"version":"1.0","ok":false,"errorType":"TokenCreationError","errorMessage":"Error creating token"}""")
      }

    }

    "list OK" taggedAs Tag("orange") in {

      val token = Injector.get[FakeTokenCreator].user

      val incomingBody =
        """
          |{
          |  "tenantId":"963995ed-ce12-4ea5-89dc-b181701d1d7b",
          |  "purpose":"King Dude - Concert",
          |  "targetIdentities":["840b7e21-03e9-4de7-bb31-0b9524f3b500"],
          |  "expiration": 2233738785,
          |  "notBefore":null
          |}
          |""".stripMargin

      post("/v1/verification/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        assert(jsonConverter.as[Good](body).right.get.isInstanceOf[Good])
      }

      post("/v1/verification/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
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

    "get single OK" taggedAs Tag("olive") in {

      val token = Injector.get[FakeTokenCreator].user

      val incomingBody =
        """
          |{
          |  "tenantId":"963995ed-ce12-4ea5-89dc-b181701d1d7b",
          |  "purpose":"King Dude - Concert",
          |  "targetIdentities":["840b7e21-03e9-4de7-bb31-0b9524f3b500"],
          |  "expiration": 2233738785,
          |  "notBefore":null
          |}
          |""".stripMargin

      post("/v1/verification/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        val bodyAsEither = jsonConverter.as[Good](body)
        val data = bodyAsEither.right.get.data.asInstanceOf[Map[String, Any]]
        val id = data.get("id").map(x => UUID.fromString(x.toString))

        assert(bodyAsEither.right.get.isInstanceOf[Good])
        assert(id.isDefined)

        get("/v1/" + id.get, headers = Map("authorization" -> token.prepare)) {
          status should equal(200)
          val res = jsonConverter.as[Good](body)
          assert(res.isRight)
          val data = res.right.get.data.asInstanceOf[Map[String, String]]
          val id2 = data.get("id").map(x => UUID.fromString(x))
          assert(id == id2)
        }

      }

    }

    "delete OK" taggedAs Tag("apple") in {

      val token = Injector.get[FakeTokenCreator].user

      val incomingBody =
        """
          |{
          |  "tenantId":"963995ed-ce12-4ea5-89dc-b181701d1d7b",
          |  "purpose":"King Dude - Concert",
          |  "targetIdentities":["840b7e21-03e9-4de7-bb31-0b9524f3b500"],
          |  "expiration": 2233738785,
          |  "notBefore":null
          |}
          |""".stripMargin

      post("/v1/verification/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        assert(jsonConverter.as[Good](body).right.get.isInstanceOf[Good])
      }

      post("/v1/verification/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
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
      post("/v1/verification/create", body = incomingBody) {
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
      post("/v1/verification/create", body = incomingBody, headers = Map("authorization" -> UUID.randomUUID().toString)) {
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

    "fail wrong *" taggedAs Tag("guava") in {

      val token = Injector.get[FakeTokenCreator].user

      val incomingBody =
        """
          |{
          |  "tenantId":"963995ed-ce12-4ea5-89dc-b181701d1d7b",
          |  "purpose":"King Dude - Concert",
          |  "targetIdentities": "other stuff",
          |  "expiration": 2233738785,
          |  "notBefore":null
          |}
          |""".stripMargin

      post("/v1/verification/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(400)
        assert(body == """{"version":"1.0","ok":false,"errorType":"TokenCreationError","errorMessage":"Error creating token"}""")
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
