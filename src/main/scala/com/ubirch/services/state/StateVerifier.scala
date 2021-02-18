package com.ubirch.services.state

import java.util.UUID

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.ConfPaths.{ ExternalStateGetterPaths, GenericConfPaths }
import com.ubirch.{ StateVerifierException, TokenEncodingException }
import com.ubirch.models.{ Group, Scopes, TokenPurposedClaim }
import com.ubirch.services.config.ConfigProvider
import com.ubirch.services.execution.{ ExecutionProvider, SchedulerProvider }
import com.ubirch.services.formats.{ DefaultJsonConverterService, JsonConverterService, JsonFormatsProvider }
import com.ubirch.services.jwt.{ DefaultTokenEncodingService, DefaultTokenKeyService, TokenEncodingService, TokenKeyService }
import com.ubirch.util.TaskHelpers
import monix.eval.Task
import monix.execution.Scheduler
import net.logstash.logback.argument.StructuredArguments.v

import javax.inject.{ Inject, Singleton }

trait StateVerifier {
  def groups(uuid: UUID): Task[List[Group]]
}

@Singleton
class DefaultStateVerifier @Inject() (
    config: Config,
    externalStateGetter: ExternalStateGetter,
    tokenKey: TokenKeyService,
    tokenEncodingService: TokenEncodingService,
    jsonConverterService: JsonConverterService
)
  extends StateVerifier with TaskHelpers with LazyLogging {

  private final val ENV = config.getString(GenericConfPaths.ENV)
  private final val REALM_NAME: String = config.getString(ExternalStateGetterPaths.REALM_NAME)

  override def groups(uuid: UUID): Task[List[Group]] = {
    for {

      _ <- Task.unit

      tokenClaim = TokenPurposedClaim(
        tenantId = uuid, // What UUID to use here?
        purpose = "systems_interchange",
        targetIdentities = Left(List(uuid)),
        expiration = Some(60 * 5),
        notBefore = None,
        originDomains = Nil,
        scopes = List(Scopes.asString(Scopes.Thing_GetInfo))
      ).toTokenClaim(ENV)
        .addContent(
          'realm_access -> "DEVICE",
          'realm_name -> REALM_NAME
        )

      res <- liftTry(tokenEncodingService.encode(UUID.randomUUID(), tokenClaim, tokenKey.key))(TokenEncodingException("Error creating token", tokenClaim))
      (token, _) = res

      res <- Task.delay(externalStateGetter.getDeviceGroups(token))

      resBody <- Task(new String(res.body))

      _ = logger.info(
        "deviceId:" + uuid.toString +
          " res_status:" + res.status +
          " res_body:" + resBody,
        v("deviceId", uuid)
      )

      groups <- Task.fromEither(jsonConverterService.as[List[Group]](resBody))
        .onErrorRecoverWith {
          case e: Exception =>
            logger.error("error_getting_groups=" + e.getMessage)
            Task.raiseError(StateVerifierException("Invalid Get Groups Response"))
        }

    } yield {
      groups
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
    val externalStateGetter = new DefaultExternalGetter(config)
    val tokenKeyService = new DefaultTokenKeyService(config)
    val tokenEncodingService = new DefaultTokenEncodingService()

    val formats = new JsonFormatsProvider() get ()
    val jsonConverterService = new DefaultJsonConverterService()(formats)

    val stateVerifier: StateVerifier = new DefaultStateVerifier(config, externalStateGetter, tokenKeyService, tokenEncodingService, jsonConverterService)

    val res = await(stateVerifier.groups(UUID.fromString("bdab47d0-fcf9-429e-a118-3dae0773cac2")), 5 seconds)

    println(res)

  }
}
