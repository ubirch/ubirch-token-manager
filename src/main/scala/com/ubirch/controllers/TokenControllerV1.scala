package com.ubirch.controllers

import java.util.UUID

import com.typesafe.config.Config
import com.ubirch.ConfPaths.GenericConfPaths
import com.ubirch.controllers.concerns.{ ControllerBase, ControllerCounters, KeycloakBearerAuthStrategy, KeycloakBearerAuthenticationSupport, SwaggerElements }
import com.ubirch.models._
import com.ubirch.services.formats.JsonConverterService
import com.ubirch.services.jwt.{ PublicKeyPoolService, TokenDecodingService, TokenKeyService, TokenService }
import com.ubirch.{ DeletingException, InvalidParamException, ServiceException }
import io.prometheus.client.Counter
import javax.inject._
import monix.eval.Task
import monix.execution.Scheduler
import org.json4s.Formats
import org.scalatra._
import org.scalatra.swagger.{ ResponseMessage, Swagger, SwaggerSupportSyntax }

import scala.concurrent.ExecutionContext

@Singleton
class TokenControllerV1 @Inject() (
    config: Config,
    val swagger: Swagger,
    jFormats: Formats,
    jsonConverterService: JsonConverterService,
    publicKeyPoolService: PublicKeyPoolService,
    tokenDecodingService: TokenDecodingService,
    tokenService: TokenService,
    tokenKeyService: TokenKeyService
)(implicit val executor: ExecutionContext, scheduler: Scheduler)
  extends ControllerBase with KeycloakBearerAuthenticationSupport {

  override val version: Symbol = Response.v1
  override protected val applicationDescription: String = "Token Controller " + version
  override protected implicit def jsonFormats: Formats = jFormats

  val service: String = config.getString(GenericConfPaths.NAME)

  val successCounter: Counter = ControllerCounters.successCounter
  val errorCounter: Counter = ControllerCounters.errorCounter

  before() {
    contentType = "application/json"
  }

  options("/*") {
    response.setHeader("Access-Control-Allow-Methods", "POST, GET, DELETE, OPTIONS")
    response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"))
    NoContent()
  }

  val postV1TokenCreate: SwaggerSupportSyntax.OperationBuilder =
    (apiOperation[TokenCreationData]("postV1TokenCreate")
      summary "Creates Generic Tokens"
      description "Creates Generic Access Tokens for particular uses. This endpoint is only valid for users that are admins."
      tags SwaggerElements.TAG_TOKEN_SERVICE_V1
      parameters (
        bodyParam[TokenClaim]("tokeClaim").description("The token claims" +
          "\n Note that expiration and notBefore can be read as follows: " +
          "\n _expiration_: the number of seconds after which the token will be considered expired. That is to say: 'X seconds from now', where X == expiration AND now == the current time calculated on the server." +
          "\n _notBefore_: the number of seconds after which the token should be considered valid. \nThat is to say: 'X seconds from now', where X == notBefore AND now == the current time calculated on the server."),
        swaggerTokenAsHeader
      )
        responseMessages (
          ResponseMessage(
            SwaggerElements.ERROR_REQUEST_CODE_400,
            jsonConverterService.toString(NOK.tokenDeleteError(version, "Error creating token"))
              .right
              .getOrElse("Error creating token")
          ),
            ResponseMessage(
              SwaggerElements.INTERNAL_ERROR_CODE_500,
              jsonConverterService.toString(NOK.serverError(version, "1.1 Sorry, something went wrong on our end"))
                .right
                .getOrElse("Sorry, something went wrong on our end")
            )
        ))

  post("/create", operation(postV1TokenCreate)) {

    authenticated(_.isAdmin) { token =>
      asyncResult("create_token") { _ => _ =>
        for {
          readBody <- Task.delay(ReadBody.readJson[TokenClaim](t => t))
          res <- tokenService.create(token, readBody.extracted, 'generic)
            .map { tkc => Ok(Good(version, tkc)) }
            .onErrorHandle {
              case e: ServiceException =>
                logger.error("1.1 Error creating token: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
                BadRequest(NOK.tokenCreationError(version, "Error creating token"))
              case e: Exception =>
                logger.error("1.2 Error creating token: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
                InternalServerError(NOK.serverError(version, "1.2 Sorry, something went wrong on our end"))
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
      tags SwaggerElements.TAG_TOKEN_SERVICE_V1
      parameters (
        bodyParam[TokenPurposedClaim]("TokenPurposedClaim").description(
          "The verification token claims. " +
            "\n Note that expiration and notBefore can be read as follows: " +
            "\n _expiration_: the number of seconds after which the token will be considered expired. That is to say: 'X seconds from now', where X == expiration AND now == the current time calculated on the server." +
            "\n _notBefore_: the number of seconds after which the token should be considered valid. \nThat is to say: 'X seconds from now', where X == notBefore AND now == the current time calculated on the server."
        ),
          swaggerTokenAsHeader
      )
          responseMessages (
            ResponseMessage(
              SwaggerElements.ERROR_REQUEST_CODE_400,
              jsonConverterService.toString(NOK.tokenDeleteError(version, "Error creating token"))
                .right
                .getOrElse("Error creating token")
            ),
              ResponseMessage(
                SwaggerElements.INTERNAL_ERROR_CODE_500,
                jsonConverterService.toString(NOK.serverError(version, "1.1 Sorry, something went wrong on our end"))
                  .right
                  .getOrElse("Sorry, something went wrong on our end")
              )
          ))

  post("/verification/create", operation(postV1TokenVerificationCreate)) {

    authenticated() { token =>
      asyncResult("create_verification_token") { _ => _ =>
        for {
          readBody <- Task.delay(ReadBody.readJson[TokenPurposedClaim](t => t.camelizeKeys))
          res <- tokenService.create(token, readBody.extracted)
            .map { tkc => Ok(Good(version, tkc)) }
            .onErrorHandle {
              case e: ServiceException =>
                logger.error("1.1 Error creating token: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
                BadRequest(NOK.tokenCreationError(version, "Error creating token"))
              case e: Exception =>
                logger.error("1.2 Error creating token: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
                InternalServerError(NOK.serverError(version, "1.2 Sorry, something went wrong on our end"))
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
      tags SwaggerElements.TAG_TOKEN_SERVICE_V1
      parameters swaggerTokenAsHeader)

  get("/", operation(getV1TokenList)) {

    authenticated() { token =>
      asyncResult("list_tokens") { _ => _ =>
        for {
          res <- tokenService.list(token)
            .map { tks => Ok(Good(version, tks)) }
            .onErrorHandle {
              case e: ServiceException =>
                logger.error("1.1 Error listing token: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
                BadRequest(NOK.tokenListingError(version, "Error getting tokens"))
              case e: Exception =>
                logger.error("1.2 Error listing token: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
                InternalServerError(NOK.serverError(version, "1.2 Sorry, something went wrong on our end"))
            }

        } yield {
          res
        }
      }
    }
  }

  val getV1Token: SwaggerSupportSyntax.OperationBuilder =
    (apiOperation[Option[TokenRow]]("getV1Token")
      summary "Queries token for a particular user."
      description "queries token based on an access token and token id"
      tags SwaggerElements.TAG_TOKEN_SERVICE_V1
      parameters swaggerTokenAsHeader)

  get("/:id", operation(getV1Token)) {

    authenticated() { token =>
      asyncResult("get_token") { _ => _ =>
        for {
          id <- Task(params.get("id"))
            .map(_.map(UUID.fromString).get) // We want to know if failed or not as soon as possible
            .onErrorHandle(_ => throw InvalidParamException("Invalid OwnerId", "Wrong owner param"))

          res <- tokenService.get(token, id)
            .map { tks => Ok(Good(version, tks.orNull)) }
            .onErrorHandle {
              case e: ServiceException =>
                logger.error("1.1 Error getting token: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
                BadRequest(NOK.tokenListingError(version, "Error getting token"))
              case e: Exception =>
                logger.error("1.2 Error getting token: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
                InternalServerError(NOK.serverError(version, "1.2 Sorry, something went wrong on our end"))
            }

        } yield {
          res
        }
      }
    }
  }

  val getV1JWK: SwaggerSupportSyntax.OperationBuilder =
    (apiOperation[Map[String, String]]("getV1JWK")
      summary "Returns the public key used to verify tokens in jwk format"
      description "returns the jwk for the current token verification"
      tags SwaggerElements.TAG_TOKEN_SERVICE_V1)

  get("/jwk", operation(getV1JWK)) {
    asyncResult("get_jwk") { _ => _ =>
      (for {
        key <- Task.fromEither(jsonConverterService.as[Map[String, String]](tokenKeyService.publicJWK.toJson))
        res <- Task.delay(Ok(Good(version, key)))
      } yield {
        res
      }).onErrorRecover {
        case e: Exception =>
          logger.error("1.1 Error getting jwk: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
          InternalServerError(NOK.serverError(version, "1.1 Sorry, something went wrong on our end"))
      }
    }
  }

  val deleteV1TokenId: SwaggerSupportSyntax.OperationBuilder =
    (apiOperation[Good]("deleteV1TokenId")
      summary "Deletes a token"
      description "deletes a token"
      tags SwaggerElements.TAG_TOKEN_SERVICE_V1
      parameters (
        swaggerTokenAsHeader,
        bodyParam[String]("tokenId").description("the token to delete")
      )
        responseMessages (
          ResponseMessage(
            SwaggerElements.ERROR_REQUEST_CODE_400,
            jsonConverterService.toString(NOK.tokenDeleteError(version, "Error deleting token"))
              .right
              .getOrElse("Error deleting token")
          ),
            ResponseMessage(
              SwaggerElements.INTERNAL_ERROR_CODE_500,
              jsonConverterService.toString(NOK.serverError(version, "1.1 Sorry, something went wrong on our end"))
                .right
                .getOrElse("Sorry, something went wrong on our end")
            )
        ))

  delete("/:tokenId", operation(deleteV1TokenId)) {
    authenticated() { accessToken =>

      asyncResult("delete") { implicit request => _ =>

        for {
          tokenId <- Task(params.get("tokenId"))
            .map(_.map(UUID.fromString).get) // We want to know if failed or not as soon as possible
            .onErrorHandle(_ => throw DeletingException("Invalid Token", "No tokenId parameter found in path"))

          res <- tokenService.delete(accessToken, tokenId)
            .map { dr =>
              if (dr) Ok(Good(version, "Token deleted"))
              else BadRequest(NOK.tokenDeleteError(version, "Failed to delete token"))
            }
            .onErrorRecover {
              case e: ServiceException =>
                logger.error("1.1 Error deleting token: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
                BadRequest(NOK.tokenDeleteError(version, "Error deleting token"))
              case e: Exception =>
                logger.error("1.1 Error deleting token: exception={} message={}", e.getClass.getCanonicalName, e.getMessage)
                InternalServerError(NOK.serverError(version, "1.1 Sorry, something went wrong on our end"))
            }

        } yield {
          res
        }

      }

    }
  }

  notFound {
    asyncResult("not_found") { _ => _ =>
      Task {
        logger.info("controller=TokenController route_not_found={} query_string={}", requestPath, request.getQueryString)
        NotFound(NOK.noRouteFound(version, requestPath + " might exist in another universe"))
      }
    }
  }

  override protected def createStrategy(app: ScalatraBase): KeycloakBearerAuthStrategy = {
    new KeycloakBearerAuthStrategy(app, tokenDecodingService, publicKeyPoolService)
  }

  def swaggerTokenAsHeader: SwaggerSupportSyntax.ParameterBuilder[String] = headerParam[String]("Authorization")
    .description("Token of the user. ADD \"bearer \" followed by a space) BEFORE THE TOKEN OTHERWISE IT WON'T WORK")

}
