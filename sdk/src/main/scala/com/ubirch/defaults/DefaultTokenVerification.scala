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

  private val validIssuer = config.getString(Paths.VALID_ISSUER_PATH)
  private val validAudience = config.getString(Paths.VALID_AUDIENCE_PATH)
  private val validScopes = config.getStringList(Paths.VALID_SCOPES_PATH).asScala.toList

  override def decodeAndVerify(jwt: String): Try[Claims] = {
    (for {

      (_, p, _) <- Jwt.decodeRawAll(jwt, tokenPublicKey.publicKey, Seq(JwtAlgorithm.ES256))

      all <- jsonConverterService.toJValue(p).toTry
        .recover { case e: Exception => throw InvalidAllClaims(e.getMessage, jwt) }

      claims = new Claims(jwt, all)

      isIssuerValid <- Try(claims.issuer).map(_ == validIssuer)
      _ = if (!isIssuerValid) throw InvalidSpecificClaim("Invalid issuer", p)

      isAudienceValid <- Try(claims.audiences).map(_.contains(validAudience))
      _ = if (!isAudienceValid) throw InvalidSpecificClaim("Invalid audience", p)

      _ <- Try(claims.subject)
        .filter(_.nonEmpty)
        .map(UUID.fromString)
        .recover { case e: Exception => throw InvalidSpecificClaim(e.getMessage, p) }

      _ <- Try(claims.scopes).filter(_.exists(validScopes.contains))
        .recover { case _: Exception => throw InvalidSpecificClaim(s"Invalid Scopes :: ${claims.scopes} not found in $validScopes", p) }

      _ <- Try(claims.purpose).filter(_.nonEmpty)
        .recover { case e: Exception => throw InvalidSpecificClaim(e.getMessage, p) }

    } yield {
      claims
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
