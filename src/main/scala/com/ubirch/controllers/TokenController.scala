package com.ubirch.controllers

import com.typesafe.config.Config
import com.ubirch.ConfPaths.GenericConfPaths
import com.ubirch.controllers.concerns.{ControllerBase, SwaggerElements}
import com.ubirch.services.jwt.TokenCreation
import io.prometheus.client.Counter
import javax.inject._
import monix.eval.Task
import monix.execution.Scheduler
import org.json4s.Formats
import org.scalatra._
import org.scalatra.swagger.{Swagger, SwaggerSupportSyntax}

import scala.concurrent.ExecutionContext

@Singleton
class TokenController @Inject()(config: Config, val swagger: Swagger, jFormats: Formats, tokenCreation: TokenCreation)(implicit val executor: ExecutionContext, scheduler: Scheduler)
  extends ControllerBase {

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

  get("/v1", operation(getSimpleCheck)) {
    asyncResult("create_token") { _ =>

//      for {
//        readBody <- Task.delay(ReadBody.readJson[TokenClaim](t => t))
//        res <- tokenCreation.encode(readBody.extracted)
//          .map { key => Ok(key) }
//          .onErrorHandle {
//            case e: PubKeyServiceException =>
//              logger.error("1.1 Error creating pub key: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
//              BadRequest(NOK.pubKeyError("Error creating pub key"))
//            case e: Exception =>
//              logger.error("1.2 Error creating pub key: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
//              InternalServerError(NOK.serverError("1.2 Sorry, something went wrong on our end"))
//          }
//
//      } yield {
//        res
//      }

      Task.delay(Ok(""))

    }
  }



}
