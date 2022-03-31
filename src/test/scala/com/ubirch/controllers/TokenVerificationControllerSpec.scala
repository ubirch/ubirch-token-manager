package com.ubirch.controllers

import java.security.PublicKey
import java.util.UUID

import com.ubirch.models.{ Return, VerificationRequest }
import com.ubirch.services.formats.JsonConverterService
import com.ubirch.services.jwt.{ PublicKeyPoolService, TokenDecodingService }
import com.ubirch.{ EmbeddedCassandra, _ }
import io.prometheus.client.CollectorRegistry
import org.jose4j.jwk.PublicJsonWebKey
import org.json4s.JsonAST.{ JArray, JField, JObject, JString }
import org.scalatest.{ BeforeAndAfterEach, Tag }
import org.scalatra.test.scalatest.ScalatraWordSpec

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{ Failure, Success }

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
  private val tokenDecodingService = Injector.get[TokenDecodingService]

  "Token Manager -Verification Tokens-" must {

    "create OK with *" taggedAs Tag("plum") in {

      val token = Injector.get[FakeTokenCreator].user

      val incomingBody =
        """
          |{
          |  "tenantId":"963995ed-ce12-4ea5-89dc-b181701d1d7b",
          |  "purpose":"King Dude - Concert",
          |  "targetIdentities": ["*"],
          |  "expiration": 2233738785,
          |  "notBefore":null,
          |  "scopes" : ["upp:verify"]
          |}
          |""".stripMargin

      post("/v2/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        val b = jsonConverter.as[Return](body).right.get
        val data = b.data.asInstanceOf[Map[String, Any]]

        data.get("token") match {
          case Some(token: String) =>

            val key = PublicJsonWebKey.Factory.newPublicJwk("""{"kty":"EC","x":"Lgn8c96LBnxMOCkujWg-06uu8iDJuKa4WTWgVTWROac","y":"Dxey52VDUYoRP7qEhj22BguwIk_EUQTKCsioJ5sNdEo","crv":"P-256"}""").getKey
            tokenDecodingService.decodeAndVerify(token, key) match {
              case Failure(exception) => fail(exception)
              case Success(value) =>

                def complete(completeWith: Boolean = false)(list: List[Boolean]): List[Boolean] = list match {
                  case Nil => List(completeWith)
                  case _ => list
                }

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
                  JField("tid", JArray(tid)) <- child
                  JString("*") <- tid
                } yield true)

                val tgpOK = complete()(for {
                  JObject(child) <- value
                  JField("tgp", JArray(tid)) <- child
                } yield tid.isEmpty)

                val ordOK = complete()(for {
                  JObject(child) <- value
                  JField("ord", JArray(tid)) <- child
                } yield tid.isEmpty)

                val purposeOk = complete()(for {
                  JObject(child) <- value
                  JField("pur", JString("King Dude - Concert")) <- child
                } yield true)

                val scopesOk = complete()(for {
                  JObject(child) <- value
                  JField("scp", JArray(tid)) <- child
                  JString("upp:verify") <- tid
                } yield true)

                val check = issOK ++ audOK ++ subOK ++ tidOK ++ tgpOK ++ ordOK ++ purposeOk ++ scopesOk

                assert(check.forall(b => b) && check.nonEmpty)

            }

          case _ => fail("No Token Found")
        }

        assert(b.isInstanceOf[Return])
      }

    }

    "create OK with list of domains" taggedAs Tag("pomegranate") in {

      val token = Injector.get[FakeTokenCreator].user

      val incomingBody =
        """
          |{
          |  "tenantId":"963995ed-ce12-4ea5-89dc-b181701d1d7b",
          |  "purpose":"King Dude - Concert",
          |  "targetIdentities": ["*"],
          |  "expiration": 2233738785,
          |  "notBefore":null,
          |  "originDomains": ["https://meet.google.com", "https://simple.wikipedia.org"],
          |  "scopes" : ["upp:verify"]
          |}
          |""".stripMargin

      post("/v2/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        assert(jsonConverter.as[Return](body).right.get.isInstanceOf[Return])
      }

    }

    "not create when schema is wrong" taggedAs Tag("pomegranate") in {

      val token = Injector.get[FakeTokenCreator].user

      val incomingBody =
        """
          |{
          |  "tenantId":"963995ed-ce12-4ea5-89dc-b181701d1d7b",
          |  "purpose":"King Dude - Concert",
          |  "targetIdentities": ["*"],
          |  "expiration": 2233738785,
          |  "notBefore":null,
          |  "originDomains": ["ftp://meet.google.com", "https://simple.wikipedia.org"]
          |}
          |""".stripMargin

      post("/v2/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(400)
        assert(body == """{"version":"2.0.0","ok":false,"errorType":"TokenCreationError","errorMessage":"Error creating token:Invalid Origin Domains"}""")
      }

    }

    "create OK and Verify should fail if Needed headers are not found or are empty" taggedAs Tag("avocado") in {

      val token = Injector.get[FakeTokenCreator].user

      val incomingBody =
        """
          |{
          |  "tenantId":"963995ed-ce12-4ea5-89dc-b181701d1d7b",
          |  "purpose":"King Dude - Concert",
          |  "targetIdentities":["840b7e21-03e9-4de7-bb31-0b9524f3b500"],
          |  "expiration": 2233738785,
          |  "notBefore":null,
          |  "scopes" : ["upp:verify"]
          |}
          |""".stripMargin

      post("/v2/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(200)

        val b = jsonConverter.as[Return](body).right.get
        val data = b.data.asInstanceOf[Map[String, Any]]

        data.get("token") match {
          case Some(token: String) =>

            val verificationRequest = VerificationRequest(token, UUID.fromString("840b7e21-03e9-4de7-bb31-0b9524f3b500"), None, None, None)
            val verificationRequestJson = jsonConverter.toString[VerificationRequest](verificationRequest).getOrElse("")

            post("/v2/verify", body = verificationRequestJson, headers = Map("X-Ubirch-Signature" -> "", "X-Ubirch-Timestamp" -> "111")) {
              assert(status == 400)
              assert(body == """{"version":"2.0.0","ok":false,"errorType":"TokenVerificationError","errorMessage":"Error verifying token"}""".stripMargin)
            }

          case _ => fail("No Token Found")
        }

        assert(jsonConverter.as[Return](body).right.get.isInstanceOf[Return])
      }

    }

    "create OK and Verify should fail if Token not provided" taggedAs Tag("avocado") in {

      val token = Injector.get[FakeTokenCreator].user

      val incomingBody =
        """
          |{
          |  "tenantId":"963995ed-ce12-4ea5-89dc-b181701d1d7b",
          |  "purpose":"King Dude - Concert",
          |  "targetIdentities":["840b7e21-03e9-4de7-bb31-0b9524f3b500"],
          |  "expiration": 2233738785,
          |  "notBefore":null,
          |  "scopes" : ["upp:verify"]
          |}
          |""".stripMargin

      post("/v2/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(200)

        val b = jsonConverter.as[Return](body).right.get
        val data = b.data.asInstanceOf[Map[String, Any]]

        data.get("token") match {
          case Some(_: String) =>

            val verificationRequest = VerificationRequest("", UUID.fromString("840b7e21-03e9-4de7-bb31-0b9524f3b500"), None, None, None)
            val verificationRequestJson = jsonConverter.toString[VerificationRequest](verificationRequest).getOrElse("")

            post("/v2/verify", body = verificationRequestJson, headers = Map("X-Ubirch-Signature" -> "111", "X-Ubirch-Timestamp" -> "111")) {
              assert(status == 400)
              assert(body == """{"version":"2.0.0","ok":false,"errorType":"TokenVerificationError","errorMessage":"Error verifying token"}""".stripMargin)
            }

          case _ => fail("No Token Found")
        }

        assert(jsonConverter.as[Return](body).right.get.isInstanceOf[Return])
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
          |  "notBefore":null,
          |  "scopes" : ["upp:verify"]
          |}
          |""".stripMargin

      post("/v2/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(200)

        val b = jsonConverter.as[Return](body).right.get
        val data = b.data.asInstanceOf[Map[String, Any]]

        data.get("token") match {
          case Some(token: String) =>

            val key = PublicJsonWebKey.Factory.newPublicJwk("""{"kty":"EC","x":"Lgn8c96LBnxMOCkujWg-06uu8iDJuKa4WTWgVTWROac","y":"Dxey52VDUYoRP7qEhj22BguwIk_EUQTKCsioJ5sNdEo","crv":"P-256"}""").getKey
            tokenDecodingService.decodeAndVerify(token, key.asInstanceOf[PublicKey]) match {
              case Failure(exception) => fail(exception)
              case Success(value) =>

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
                  JField("tid", JArray(tid)) <- child
                  JString("840b7e21-03e9-4de7-bb31-0b9524f3b500") <- tid
                } yield true)

                val ordOK = complete()(for {
                  JObject(child) <- value
                  JField("ord", JArray(tid)) <- child
                } yield tid.isEmpty)

                val purposeOk = complete()(for {
                  JObject(child) <- value
                  JField("pur", JString("King Dude - Concert")) <- child
                } yield true)

                val scopesOk = complete()(for {
                  JObject(child) <- value
                  JField("scp", JArray(tid)) <- child
                  JString("upp:verify") <- tid
                } yield true)

                val check = issOK ++ audOK ++ subOK ++ tidOK ++ ordOK ++ purposeOk ++ scopesOk

                assert(check.forall(b => b) && check.nonEmpty)

            }

          case _ => fail("No Token Found")
        }

        assert(jsonConverter.as[Return](body).right.get.isInstanceOf[Return])
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

      post("/v2/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(400)
        assert(body == """{"version":"2.0.0","ok":false,"errorType":"TokenCreationError","errorMessage":"Error creating token:Invalid Scopes"}""")
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
          |  "notBefore":null,
          |  "scopes" : ["upp:verify"]
          |}
          |""".stripMargin

      post("/v2/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        assert(jsonConverter.as[Return](body).right.get.isInstanceOf[Return])
      }

      post("/v2/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        assert(jsonConverter.as[Return](body).right.get.isInstanceOf[Return])
      }

      get("/v2", headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        val res = jsonConverter.as[Return](body)
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
          |  "notBefore":null,
          |  "scopes" : ["upp:verify"]
          |}
          |""".stripMargin

      post("/v2/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        val bodyAsEither = jsonConverter.as[Return](body)
        val data = bodyAsEither.right.get.data.asInstanceOf[Map[String, Any]]
        val id = data.get("id").map(x => UUID.fromString(x.toString))

        assert(bodyAsEither.right.get.isInstanceOf[Return])
        assert(id.isDefined)

        get("/v2/" + id.get, headers = Map("authorization" -> token.prepare)) {
          status should equal(200)
          val res = jsonConverter.as[Return](body)
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
          |  "notBefore":null,
          |  "scopes" : ["upp:verify"]
          |}
          |""".stripMargin

      post("/v2/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        assert(jsonConverter.as[Return](body).right.get.isInstanceOf[Return])
      }

      post("/v2/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        assert(jsonConverter.as[Return](body).right.get.isInstanceOf[Return])
      }

      var current: List[Map[String, String]] = Nil
      get("/v2", headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        val res = jsonConverter.as[Return](body)
        assert(res.isRight)
        val data = res.right.get.data.asInstanceOf[List[Map[String, String]]]
        assert(data.size == 2)
        current = data
      }

      val toDelete = current.headOption.flatMap(_.find(_._1 == "id")).map(_._2)
      assert(toDelete.isDefined)
      delete("/v2/" + toDelete.get, headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        assert(body == """{"version":"2.0.0","ok":true,"data":"Token deleted"}""")
      }

      get("/v2", headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        val res = jsonConverter.as[Return](body)
        assert(res.isRight)
        val data = res.right.get.data.asInstanceOf[List[Map[String, String]]]
        assert(data.size == 1)
      }

    }

    "fail when no access token provided: create" taggedAs Tag("mandarina") in {

      val incomingBody = "{}"
      post("/v2/create", body = incomingBody) {
        status should equal(401)
        assert(body == """{"version":"2.0.0","ok":false,"errorType":"AuthenticationError","errorMessage":"Unauthenticated"}""")
      }

    }

    "fail when no access token provided: list" taggedAs Tag("avocado") in {

      get("/v2") {
        status should equal(401)
        assert(body == """{"version":"2.0.0","ok":false,"errorType":"AuthenticationError","errorMessage":"Unauthenticated"}""")
      }

    }

    "fail when no access token provided: delete" taggedAs Tag("strawberry") in {

      delete("/v2/" + UUID.randomUUID().toString) {
        status should equal(401)
        assert(body == """{"version":"2.0.0","ok":false,"errorType":"AuthenticationError","errorMessage":"Unauthenticated"}""")
      }

    }

    "fail when invalid access token provided: create" taggedAs Tag("durian") in {

      val incomingBody = "{}"
      post("/v2/create", body = incomingBody, headers = Map("authorization" -> UUID.randomUUID().toString)) {
        status should equal(400)
        assert(body == """{"version":"2.0.0","ok":false,"errorType":"AuthenticationError","errorMessage":"Invalid bearer token"}""")
      }

    }

    "fail when invalid access token provided: list" taggedAs Tag("cherry") in {

      get("/v2", headers = Map("authorization" -> UUID.randomUUID().toString)) {
        status should equal(400)
        assert(body == """{"version":"2.0.0","ok":false,"errorType":"AuthenticationError","errorMessage":"Invalid bearer token"}""")
      }

    }

    "fail when invalid access token provided: delete" taggedAs Tag("blackberry") in {

      delete("/v2/" + UUID.randomUUID().toString, headers = Map("authorization" -> UUID.randomUUID().toString)) {
        status should equal(400)
        assert(body == """{"version":"2.0.0","ok":false,"errorType":"AuthenticationError","errorMessage":"Invalid bearer token"}""")
      }

    }

    "fail wrong *" taggedAs Tag("guava") in {

      val token = Injector.get[FakeTokenCreator].user

      val incomingBody =
        """
          |{
          |  "tenantId":"963995ed-ce12-4ea5-89dc-b181701d1d7b",
          |  "purpose":"King Dude - Concert",
          |  "targetIdentities": ["other stuff"],
          |  "expiration": 2233738785,
          |  "notBefore":null
          |}
          |""".stripMargin

      post("/v2/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(400)
        assert(body == """{"version":"2.0.0","ok":false,"errorType":"TokenCreationError","errorMessage":"Error creating token:Invalid Target Identities"}""")
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

    lazy val tokenController = Injector.get[TokenController]

    addServlet(tokenController, "/*")

    super.beforeAll()
  }
}
