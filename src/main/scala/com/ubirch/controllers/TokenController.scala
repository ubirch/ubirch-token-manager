package com.ubirch.controllers

import com.typesafe.config.Config
import com.ubirch.ConfPaths.GenericConfPaths
import com.ubirch.ServiceException
import com.ubirch.controllers.concerns.{ BearerAuthStrategy, BearerAuthenticationSupport, ControllerBase, KeycloakBearerAuthStrategy, KeycloakBearerAuthenticationSupport, SwaggerElements }
import com.ubirch.models.{ NOK, TokenClaim }
import com.ubirch.services.jwt.{ PublicKeyDiscoveryService, PublicKeyPoolService, TokenStoreService, TokenVerificationService }
import io.prometheus.client.Counter
import javax.inject._
import javax.servlet.http.HttpServletRequest
import monix.eval.Task
import monix.execution.Scheduler
import org.json4s.Formats
import org.scalatra._
import org.scalatra.swagger.{ Swagger, SwaggerSupportSyntax }

import scala.concurrent.ExecutionContext

@Singleton
class TokenController @Inject() (
    config: Config,
    val swagger: Swagger,
    jFormats: Formats,
    publicKeyPoolService: PublicKeyPoolService,
    tokenVerificationService: TokenVerificationService,
    tokenStoreService: TokenStoreService
)(implicit val executor: ExecutionContext, scheduler: Scheduler)
  extends ControllerBase with KeycloakBearerAuthenticationSupport {

  import BearerAuthStrategy.request2BearerAuthRequest

  override protected val applicationDescription = "Token Controller"
  override protected implicit def jsonFormats: Formats = jFormats

  val service: String = config.getString(GenericConfPaths.NAME)

  val successCounter: Counter = Counter.build()
    .name("token_management_success")
    .help("Represents the number of token management successes")
    .labelNames("service", "method")
    .register()

  val errorCounter: Counter = Counter.build()
    .name("token_management_failures")
    .help("Represents the number of token management failures")
    .labelNames("service", "method")
    .register()

  val getSimpleCheck: SwaggerSupportSyntax.OperationBuilder =
    (apiOperation[String]("simpleCheck")
      summary "Welcome"
      description "Getting a hello from the system"
      tags SwaggerElements.TAG_WELCOME)

  before() {
    contentType = "application/json"
  }

  get("/v1/test") {
    authenticated { token =>
      Ok(token)
    }
  }

  post("/v1", operation(getSimpleCheck)) {
    asyncResult("create_token") { _ =>
      for {
        readBody <- Task.delay(ReadBody.readJson[TokenClaim](t => t))
        res <- tokenStoreService.create(readBody.extracted)
          .map { tkc => Ok(tkc) }
          .onErrorHandle {
            case e: ServiceException =>
              logger.error("1.1 Error creating token: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
              BadRequest(NOK.tokenCreationError("Error creating pub key"))
            case e: Exception =>
              logger.error("1.2 Error creating token: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
              InternalServerError(NOK.serverError("1.2 Sorry, something went wrong on our end"))
          }

      } yield {
        res
      }
    }
  }

  override protected def createStrategy(app: ScalatraBase): KeycloakBearerAuthStrategy = {
    new KeycloakBearerAuthStrategy(app, tokenVerificationService, publicKeyPoolService)
  }
}
