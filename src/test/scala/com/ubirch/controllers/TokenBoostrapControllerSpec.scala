package com.ubirch.controllers

import java.nio.charset.StandardCharsets
import java.util.{ Base64, UUID }

import com.ubirch.crypto.GeneratorKeyFactory
import com.ubirch.crypto.utils.Curve
import com.ubirch.models.{ BootstrapRequest, ExternalResponseData, Return }
import com.ubirch.services.formats.JsonConverterService
import com.ubirch.services.jwt.{ PublicKeyPoolService, TokenDecodingService }
import com.ubirch.services.state.KeyGetter
import com.ubirch.util.PublicKeyUtil
import com.ubirch.{ EmbeddedCassandra, _ }
import io.prometheus.client.CollectorRegistry
import org.jose4j.jwk.PublicJsonWebKey
import org.json4s.JsonAST.{ JArray, JField, JObject, JString }
import org.scalatest.BeforeAndAfterEach
import org.scalatra.test.scalatest.ScalatraWordSpec

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{ Failure, Success }

/**
  * Test for the Key Controller
  */
class TokenBoostrapControllerSpec
  extends ScalatraWordSpec
  with EmbeddedCassandra
  with BeforeAndAfterEach
  with ExecutionContextsTests
  with Awaits {

  private val cassandra = new CassandraTest

  private lazy val Injector = new InjectorHelperImpl() {}
  private val jsonConverter = Injector.get[JsonConverterService]
  private val tokenDecodingService = Injector.get[TokenDecodingService]

  "Token Manager -Bootstrap Tokens-" must {

    "create bootstrap token" in {

      val token = Injector.get[FakeTokenCreator].user

      val incomingBody =
        """
          |{
          |  "tenantId":"963995ed-ce12-4ea5-89dc-b181701d1d7b",
          |  "purpose":"Mood Laboratories",
          |  "targetIdentities":[],
          |  "targetGroups": ["Kitchen_Carlos"],
          |  "expiration": 6311390400,
          |  "notBefore":null,
          |  "originDomains": [],
          |  "scopes": ["thing:bootstrap"]
          |}
          |""".stripMargin

      post("/v1/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
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
                  JField("aud", JString("https://token.dev.ubirch.com")) <- child
                } yield true)

                val tidOK = complete()(for {
                  JObject(child) <- value
                  JField("tid", JArray(tid)) <- child
                } yield tid.isEmpty)

                val tgpOK = complete()(for {
                  JObject(child) <- value
                  JField("tgp", JArray(tid)) <- child
                  JString("Kitchen_Carlos") <- tid
                } yield true)

                val ordOK = complete()(for {
                  JObject(child) <- value
                  JField("ord", JArray(tid)) <- child
                } yield tid.isEmpty)

                val purposeOk = complete()(for {
                  JObject(child) <- value
                  JField("pur", JString("Mood Laboratories")) <- child
                } yield true)

                val scopesOk = complete()(for {
                  JObject(child) <- value
                  JField("scp", JArray(tid)) <- child
                  JString("thing:bootstrap") <- tid
                } yield true)

                val check = issOK ++ audOK ++ subOK ++ tidOK ++ tgpOK ++ ordOK ++ purposeOk ++ scopesOk

                assert(check.forall(b => b) && check.nonEmpty)

            }

          case _ => fail("No Token Found")
        }

        assert(b.isInstanceOf[Return])
      }

    }

    "get bootstraps token" in {

      val token = Injector.get[FakeTokenCreator].user
      val fakeKeyGetter = Injector.get[KeyGetter].asInstanceOf[FakeKeyGetter]

      val incomingBody =
        """
          |{
          |  "tenantId":"963995ed-ce12-4ea5-89dc-b181701d1d7b",
          |  "purpose":"Mood Laboratories",
          |  "targetIdentities":[],
          |  "targetGroups": ["Kitchen_Carlos"],
          |  "expiration": 6311390400,
          |  "notBefore":null,
          |  "originDomains": [],
          |  "scopes": ["thing:bootstrap"]
          |}
          |""".stripMargin

      post("/v1/create", body = incomingBody, headers = Map("authorization" -> token.prepare)) {
        status should equal(200)
        val b = jsonConverter.as[Return](body).right.get
        val data = b.data.asInstanceOf[Map[String, Any]]

        data.get("token") match {
          case Some(token: String) =>

            val uuid = UUID.randomUUID()
            val privKey = GeneratorKeyFactory.getPrivKey(Curve.PRIME256V1)

            val resBody =
              s"""
                |[
                |  {
                |    "pubKeyInfo":{
                |      "algorithm":"ecdsa-p256v1",
                |      "created":"2021-01-21T10:38:45.254Z",
                |      "hwDeviceId": "${uuid.toString}",
                |      "pubKey": "${Base64.getEncoder.encodeToString(privKey.getRawPublicKey)}",
                |      "pubKeyId": "${Base64.getEncoder.encodeToString(privKey.getRawPublicKey)}",
                |      "validNotAfter":"2031-01-19T10:38:45.254Z",
                |      "validNotBefore":"2021-01-21T10:38:45.254Z"
                |    },
                |    "signature":"A Signature"
                |  }
                |]
                |""".stripMargin
            fakeKeyGetter.keys.update(uuid, ExternalResponseData(200, Map.empty, resBody.getBytes(StandardCharsets.UTF_8)))

            val bootstrapRequest = BootstrapRequest(token, uuid, None, None)
            jsonConverter.toString[BootstrapRequest](bootstrapRequest) match {
              case Left(exception) => fail(exception)
              case Right(br) =>

                val signature = Base64.getEncoder
                  .encodeToString(PublicKeyUtil.digestSHA512(privKey, br.getBytes(StandardCharsets.UTF_8)))

                post("/v1/bootstrap", body = br, headers = Map("X-Ubirch-Signature" -> signature)) {
                  status should equal(200)
                }

            }

          case _ => fail("No Token Found")
        }

        assert(b.isInstanceOf[Return])
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
