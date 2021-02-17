package com.ubirch.defaults

import java.util.UUID
import javax.inject.{ Inject, Singleton }
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.api._
import com.ubirch.utils.Paths
import pdi.jwt.{ Jwt, JwtAlgorithm }

import scala.collection.JavaConverters._

import scala.util.{ Failure, Try }

@Singleton
class DefaultTokenVerification @Inject() (
    config: Config,
    tokenPublicKey: TokenPublicKey,
    jsonConverterService: JsonConverterService
) extends TokenVerification with LazyLogging {

  import com.ubirch.api.TokenVerification._

  private val validIssuer = config.getString(Paths.VALID_ISSUER_PATH)
  private val validAudience = config.getString(Paths.VALID_AUDIENCE_PATH)
  private val validScopes = config.getStringList(Paths.VALID_SCOPES_PATH).asScala.toList

  override def decodeAndVerify(jwt: String): Try[Claims] = {
    (for {

      (_, p, _) <- Jwt.decodeRawAll(jwt, tokenPublicKey.publicKey, Seq(JwtAlgorithm.ES256))

      otherClaims <- Try(jsonConverterService.fromJsonInput[Content](p)(_.camelizeKeys))
        .recover { case e: Exception => throw InvalidOtherClaims(e.getMessage, jwt) }

      all <- jsonConverterService.as[Map[String, Any]](p).toTry
        .recover { case e: Exception => throw InvalidAllClaims(e.getMessage, jwt) }

      isIssuerValid <- all.get(ISSUER).toRight(InvalidSpecificClaim("Invalid issuer", p)).toTry.map(_ == validIssuer)
      _ = if (!isIssuerValid) throw InvalidSpecificClaim("Invalid issuer", p)

      maybeAudiences <- all.get(AUDIENCE).toRight(InvalidSpecificClaim("Invalid audience", p)).toTry
      isAudienceValid <- Try(maybeAudiences).map {
        case x: String => x == validAudience
        case x :: xs if x.isInstanceOf[String] => (x :: xs).contains(validAudience)
      }
      _ = if (!isAudienceValid) throw InvalidSpecificClaim("Invalid audience", p)

      _ <- all.get(SUBJECT).toRight(InvalidSpecificClaim("Invalid subject", p))
        .toTry
        .map(_.asInstanceOf[String])
        .filter(_.nonEmpty)
        .map(UUID.fromString)
        .recover { case e: Exception => throw InvalidSpecificClaim(e.getMessage, p) }

      _ <- Try(otherClaims.scopes).filter(_.exists(validScopes.contains))
        .recover { case e: Exception => throw InvalidSpecificClaim(e.getMessage, p) }

      _ <- Try(otherClaims.purpose).filter(_.nonEmpty)
        .recover { case e: Exception => throw InvalidSpecificClaim(e.getMessage, p) }

    } yield {
      Claims(jwt, all, otherClaims)
    }).recoverWith {
      case e: InvalidSpecificClaim =>
        logger.error(s"invalid_token_specific_claim=${e.getMessage}", e)
        Failure(e)
      case e: InvalidAllClaims =>
        logger.error(s"invalid_token_all_claims=${e.getMessage}", e)
        Failure(e)
      case e: InvalidOtherClaims =>
        logger.error(s"invalid_token_other_claims=${e.getMessage}", e)
        Failure(e)
      case e: Exception =>
        logger.error(s"invalid_token=${e.getMessage}", e)
        Failure(e)
    }

  }

}
