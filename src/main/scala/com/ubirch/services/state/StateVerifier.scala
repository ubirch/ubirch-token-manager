package com.ubirch.services.state

import java.util.UUID

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.ConfPaths.{ExternalStateGetterPaths, GenericConfPaths}
import com.ubirch.{StateVerifierException, TokenEncodingException}
import com.ubirch.models.{Group, Key, Scope, TokenPurposedClaim}
import com.ubirch.services.config.ConfigProvider
import com.ubirch.services.execution.{ExecutionProvider, SchedulerProvider}
import com.ubirch.services.formats.{DefaultJsonConverterService, JsonConverterService, JsonFormatsProvider}
import com.ubirch.services.jwt.{DefaultTokenEncodingService, DefaultTokenKeyService, TokenEncodingService, TokenKeyService}
import com.ubirch.util.TaskHelpers
import monix.eval.Task
import monix.execution.Scheduler
import net.logstash.logback.argument.StructuredArguments.v

import javax.inject.{Inject, Singleton}

trait StateVerifier {
  def identityGroups(identityUUID: UUID): Task[List[Group]]
  def tenantGroups(tenantId: UUID): Task[List[Group]]
  def identityKey(identityUUID: UUID): Task[List[Key]]
}

@Singleton
class DefaultStateVerifier @Inject() (
    config: Config,
    externalStateGetter: ExternalStateGetter,
    tokenKey: TokenKeyService,
    tokenEncodingService: TokenEncodingService,
    jsonConverterService: JsonConverterService,
    keyGetter: KeyGetter
)
  extends StateVerifier with TaskHelpers with LazyLogging {

  private final val ENV = config.getString(GenericConfPaths.ENV)
  private final val REALM_NAME: String = config.getString(ExternalStateGetterPaths.REALM_NAME)

  override def identityGroups(identityUUID: UUID): Task[List[Group]] = {
    for {

      _ <- Task.unit

      tokenClaim = TokenPurposedClaim(
        tenantId = identityUUID, // What UUID to use here?
        purpose = "systems_interchange",
        targetIdentities = Left(List(identityUUID)),
        targetGroups = Left(Nil),
        expiration = Some(60 * 5),
        notBefore = None,
        originDomains = Nil,
        scopes = List(Scope.asString(Scope.Thing_GetInfo))
      ).toTokenClaim(ENV)
        .addContent(
          'realm_access -> "DEVICE",
          'realm_name -> REALM_NAME
        )

      res <- liftTry(tokenEncodingService.encode(UUID.randomUUID(), tokenClaim, tokenKey.key))(TokenEncodingException("Error creating token", tokenClaim))
      (token, _) = res

      res <- externalStateGetter.getDeviceGroups(token)
      resBody <- Task(new String(res.body))
      _ = logger.info(
        "deviceId:" + identityUUID.toString +
          " res_status:" + res.status +
          " res_body:" + resBody,
        v("deviceId", identityUUID)
      )

      groups <- Task.fromEither(jsonConverterService.as[List[Group]](resBody))
        .onErrorRecoverWith {
          case e: Exception =>
            logger.error("error_getting_device_groups=" + e.getMessage)
            Task.raiseError(StateVerifierException("Invalid Get Device Groups Response"))
        }

    } yield {
      groups
    }
  }

  override def tenantGroups(tenantId: UUID): Task[List[Group]] = {
    for {

      _ <- Task.unit

      tokenClaim = TokenPurposedClaim(
        tenantId = tenantId, // What UUID to use here?
        purpose = "systems_interchange",
        targetIdentities = Left(Nil),
        targetGroups = Left(Nil),
        expiration = Some(60 * 5),
        notBefore = None,
        originDomains = Nil,
        scopes = List(Scope.asString(Scope.User_GetInfo))
      ).toTokenClaim(ENV)
        .addContent(
          'realm_access -> "USER",
          'realm_name -> REALM_NAME
        )

      res <- liftTry(tokenEncodingService.encode(UUID.randomUUID(), tokenClaim, tokenKey.key))(TokenEncodingException("Error creating token", tokenClaim))
      (token, _) = res

      res <- externalStateGetter.getUserGroups(token)
      resBody <- Task(new String(res.body))
      _ = logger.info(
        "ownerId:" + tenantId.toString +
          " res_status:" + res.status +
          " res_body:" + resBody,
        v("ownerId", tenantId)
      )

      groups <- Task.fromEither(jsonConverterService.as[List[Group]](resBody))
        .onErrorRecoverWith {
          case e: Exception =>
            logger.error("error_getting_user_groups=" + e.getMessage)
            Task.raiseError(StateVerifierException("Invalid Get User Groups Response"))
        }

    } yield {
      groups
    }
  }

  override def identityKey(identityUUID: UUID): Task[List[Key]] = {

    for {
      res <- keyGetter.byIdentityId(identityUUID)
      resBody <- Task(new String(res.body))

      _ = logger.info(
        "identityId:" + identityUUID.toString +
          " res_status:" + res.status +
          " res_body:" + resBody,
        v("identityId", identityUUID)
      )

      keys <- Task.delay(jsonConverterService.fromJsonInput[List[Key]](resBody)(_ \ "pubKeyInfo"))

    } yield {
      keys
    }

  }
}

object DefaultStateVerifier extends {

  import scala.concurrent.Await
  import scala.concurrent.duration._
  import scala.language.postfixOps

  def await[T](task: Task[T], atMost: Duration)(implicit scheduler: Scheduler): T = {
    val future = task.runToFuture
    Await.result(future, atMost)
  }

  def main(args: Array[String]): Unit = {

    val config = new ConfigProvider get ()
    val e = new ExecutionProvider(config) get ()
    implicit val scheduler = new SchedulerProvider(e) get ()
    val client = new DefaultHttpClient()
    val externalStateGetter = new DefaultExternalGetter(config, client)
    val tokenKeyService = new DefaultTokenKeyService(config)
    val tokenEncodingService = new DefaultTokenEncodingService()

    val formats = new JsonFormatsProvider() get ()
    val jsonConverterService = new DefaultJsonConverterService()(formats)

    val keyGetter = new DefaultKeyGetter(config, client)

    val stateVerifier: StateVerifier = new DefaultStateVerifier(config, externalStateGetter, tokenKeyService, tokenEncodingService, jsonConverterService, keyGetter)

    //val res = await(stateVerifier.groups(UUID.fromString("bdab47d0-fcf9-429e-a118-3dae0773cac2")), 5 seconds)

    //val res = await(stateVerifier.tenantGroups(UUID.fromString("963995ed-ce12-4ea5-89dc-b181701d1d7b")), 5 seconds)

    val res = await(stateVerifier.identityKey(UUID.fromString("9011a2de-8c69-45be-bc47-60fd58e121ce")), 5 seconds)

    println(res)

  }

}
