package com.ubirch.controllers

import java.util.UUID

import com.typesafe.config.Config
import com.ubirch.ConfPaths.GenericConfPaths
import com.ubirch.controllers.concerns.{ ControllerBase, KeycloakBearerAuthStrategy, KeycloakBearerAuthenticationSupport, SwaggerElements }
import com.ubirch.models._
import com.ubirch.services.formats.JsonConverterService
import com.ubirch.services.jwt.{ PublicKeyPoolService, TokenStoreService, TokenVerificationService }
import com.ubirch.{ DeletingException, ServiceException }
import io.prometheus.client.Counter
import javax.inject._
import monix.eval.Task
import monix.execution.Scheduler
import org.json4s.Formats
import org.scalatra._
import org.scalatra.swagger.{ ResponseMessage, Swagger, SwaggerSupportSyntax }

import scala.concurrent.ExecutionContext

@Singleton
class TokenController @Inject() (
    config: Config,
    val swagger: Swagger,
    jFormats: Formats,
    jsonConverterService: JsonConverterService,
    publicKeyPoolService: PublicKeyPoolService,
    tokenVerificationService: TokenVerificationService,
    tokenStoreService: TokenStoreService
)(implicit val executor: ExecutionContext, scheduler: Scheduler)
  extends ControllerBase with KeycloakBearerAuthenticationSupport {

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

  before() {
    contentType = "application/json"
  }

  val postV1TokenCreate: SwaggerSupportSyntax.OperationBuilder =
    (apiOperation[TokenCreationData]("postV1TokenCreate")
      summary "Creates an Access Token"
      description "Creates Generic Access Tokens for particular users"
      tags SwaggerElements.TAG_TOKEN_SERVICE
      parameters (
        bodyParam[TokenClaim]("tokeClaim").description("The token claims"),
        swaggerTokenAsHeader
      )
        responseMessages (
          ResponseMessage(
            SwaggerElements.ERROR_REQUEST_CODE_400,
            jsonConverterService.toString(NOK.tokenDeleteError("Error creating token"))
              .right
              .getOrElse("Error creating token")
          ),
            ResponseMessage(
              SwaggerElements.INTERNAL_ERROR_CODE_500,
              jsonConverterService.toString(NOK.serverError("1.1 Sorry, something went wrong on our end"))
                .right
                .getOrElse("Sorry, something went wrong on our end")
            )
        ))

  post("/v1/create", operation(postV1TokenCreate)) {

    authenticated { token =>
      asyncResult("create_token") { _ =>
        for {
          readBody <- Task.delay(ReadBody.readJson[TokenClaim](t => t))
          res <- tokenStoreService.create(token, readBody.extracted, 'generic)
            .map { tkc => Ok(Good(tkc)) }
            .onErrorHandle {
              case e: ServiceException =>
                logger.error("1.1 Error creating token: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
                BadRequest(NOK.tokenCreationError("Error creating token"))
              case e: Exception =>
                logger.error("1.2 Error creating token: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
                InternalServerError(NOK.serverError("1.2 Sorry, something went wrong on our end"))
            }

        } yield {
          res
        }
      }
    }
  }

  val postV1TokenVerificationCreate: SwaggerSupportSyntax.OperationBuilder =
    (apiOperation[TokenCreationData]("postV1TokenVerificationCreate")
      summary "Creates an Verification Access Token"
      description "Creates Verification Access Tokens for particular users"
      tags SwaggerElements.TAG_TOKEN_SERVICE
      parameters (
        bodyParam[TokenVerificationClaim]("TokenVerificationClaim").description("The verification token claims"),
        swaggerTokenAsHeader
      )
        responseMessages (
          ResponseMessage(
            SwaggerElements.ERROR_REQUEST_CODE_400,
            jsonConverterService.toString(NOK.tokenDeleteError("Error creating token"))
              .right
              .getOrElse("Error creating token")
          ),
            ResponseMessage(
              SwaggerElements.INTERNAL_ERROR_CODE_500,
              jsonConverterService.toString(NOK.serverError("1.1 Sorry, something went wrong on our end"))
                .right
                .getOrElse("Sorry, something went wrong on our end")
            )
        ))

  post("/v1/verification/create", operation(postV1TokenVerificationCreate)) {

    authenticated { token =>
      asyncResult("create_verification_token") { _ =>
        for {
          readBody <- Task.delay(ReadBody.readJson[TokenVerificationClaim](t => t.camelizeKeys))
          res <- tokenStoreService.create(token, readBody.extracted)
            .map { tkc => Ok(Good(tkc)) }
            .onErrorHandle {
              case e: ServiceException =>
                logger.error("1.1 Error creating token: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
                BadRequest(NOK.tokenCreationError("Error creating token"))
              case e: Exception =>
                logger.error("1.2 Error creating token: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
                InternalServerError(NOK.serverError("1.2 Sorry, something went wrong on our end"))
            }

        } yield {
          res
        }
      }
    }
  }

  val getV1TokenList: SwaggerSupportSyntax.OperationBuilder =
    (apiOperation[Seq[TokenRow]]("getV1TokenList")
      summary "Queries all currently valid tokens for a particular user."
      description "queries all currently valid tokens based on an access token"
      tags SwaggerElements.TAG_TOKEN_SERVICE
      parameters swaggerTokenAsHeader)

  get("/v1", operation(getV1TokenList)) {

    authenticated { token =>
      asyncResult("list_tokens") { _ =>
        for {
          res <- tokenStoreService.list(token)
            .map { tks => Ok(Good(tks)) }
            .onErrorHandle {
              case e: ServiceException =>
                logger.error("1.1 Error listing token: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
                BadRequest(NOK.tokenListingError("Error creating token"))
              case e: Exception =>
                logger.error("1.2 Error listing token: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
                InternalServerError(NOK.serverError("1.2 Sorry, something went wrong on our end"))
            }

        } yield {
          res
        }
      }
    }
  }

  val deleteV1TokenId: SwaggerSupportSyntax.OperationBuilder =
    (apiOperation[Good]("deleteV1TokenId")
      summary "Deletes a token"
      description "deletes a token"
      tags SwaggerElements.TAG_TOKEN_SERVICE
      parameters (
        swaggerTokenAsHeader,
        bodyParam[String]("tokenId").description("the token to delete")
      )
        responseMessages (
          ResponseMessage(
            SwaggerElements.ERROR_REQUEST_CODE_400,
            jsonConverterService.toString(NOK.tokenDeleteError("Error deleting token"))
              .right
              .getOrElse("Error deleting token")
          ),
            ResponseMessage(
              SwaggerElements.INTERNAL_ERROR_CODE_500,
              jsonConverterService.toString(NOK.serverError("1.1 Sorry, something went wrong on our end"))
                .right
                .getOrElse("Sorry, something went wrong on our end")
            )
        ))

  delete("/v1/:tokenId", operation(deleteV1TokenId)) {
    authenticated { accessToken =>

      asyncResult("delete") { implicit request =>

        for {
          tokenIdAsString <- Task(params.getOrElse("tokenId", throw DeletingException("Invalid Token", "No tokenId parameter found in path")))
          tokenId <- Task(UUID.fromString(tokenIdAsString))
          res <- tokenStoreService.delete(accessToken, tokenId)
            .map { dr =>
              if (dr) Ok(Good("Token deleted"))
              else BadRequest(NOK.tokenDeleteError("Failed to delete token"))
            }
            .onErrorRecover {
              case e: ServiceException =>
                logger.error("1.1 Error deleting token: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
                BadRequest(NOK.tokenDeleteError("Error deleting token"))
              case e: Exception =>
                logger.error("1.1 Error deleting token: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
                InternalServerError(NOK.serverError("1.1 Sorry, something went wrong on our end"))
            }

        } yield {
          res
        }

      }

    }
  }

  notFound {
    asyncResult("not_found") { _ =>
      Task {
        logger.info("controller=TokenController route_not_found={} query_string={}", requestPath, request.getQueryString)
        NotFound(NOK.noRouteFound(requestPath + " might exist in another universe"))
      }
    }
  }

  override protected def createStrategy(app: ScalatraBase): KeycloakBearerAuthStrategy = {
    new KeycloakBearerAuthStrategy(app, tokenVerificationService, publicKeyPoolService)
  }

  def swaggerTokenAsHeader: SwaggerSupportSyntax.ParameterBuilder[String] = headerParam[String]("Authorization")
    .description("Token of the user. ADD \"bearer \" followed by a space) BEFORE THE TOKEN OTHERWISE IT WON'T WORK")

}
