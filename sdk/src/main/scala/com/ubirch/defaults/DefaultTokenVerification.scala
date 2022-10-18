package com.ubirch.defaults

import java.util.UUID
import javax.inject.{ Inject, Singleton }
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.api._
import com.ubirch.utils.ConfigHelper
import com.typesafe.config.ConfigException.WrongType
import pdi.jwt.{ Jwt, JwtAlgorithm }

import scala.util.{ Failure, Try }

@Singleton
class DefaultTokenVerification @Inject() (
    config: Config,
    tokenPublicKey: TokenPublicKey,
    jsonConverterService: JsonConverterService
) extends TokenVerification with LazyLogging {

  private val validIssuer: String = config.getString(Paths.VALID_ISSUER_PATH)
  private val validAudiences: Set[String] = {
    Try(ConfigHelper.getStringList(config, Paths.VALID_AUDIENCE_PATH).toSet)
      .recover {
        case _: WrongType => Set(config.getString(Paths.VALID_AUDIENCE_PATH))
      }.get
  }
  private val validScopes: Set[String] = ConfigHelper.getStringList(config, Paths.VALID_SCOPES_PATH).toSet

  override def decodeAndVerify(jwt: String): Try[Claims] = {
    (for {

      _ <- Try(jwt).filter(_.nonEmpty).recover { case _: Exception =>
        throw new IllegalArgumentException("Token can't be empty")
      }

      (_, p, _) <- Jwt.decodeRawAll(jwt, tokenPublicKey.publicKey, Seq(JwtAlgorithm.ES256))

      all <- jsonConverterService.toJValue(p).toTry
        .recover { case e: Exception => throw InvalidClaimException(e.getMessage, jwt) }

      claims = new Claims(jwt, all)

      isIssuerValid <- Try(claims.issuer).map(_ == validIssuer)
      _ = if (!isIssuerValid) throw InvalidClaimException("Invalid issuer", p)

      _ <- Try(claims.audiences).filter(_.exists(validAudiences.contains))
        .recover { case _: Exception => throw InvalidClaimException(s"Invalid audience :: ${claims.audiences.mkString(",")} not found in ${validAudiences.mkString(",")}", p) }

      _ <- Try(claims.subject)
        .filter(_.nonEmpty)
        .map(UUID.fromString)
        .recover { case e: Exception => throw InvalidClaimException(e.getMessage, p) }

      _ <- Try(claims.scopes).filter(_.exists(validScopes.contains))
        .recover { case _: Exception => throw InvalidClaimException(s"Invalid Scopes :: ${claims.scopes.mkString(",")} not found in ${validScopes.mkString(",")}", p) }

      _ <- Try(claims.purpose).filter(_.nonEmpty)
        .recover { case e: Exception => throw InvalidClaimException(e.getMessage, p) }

    } yield {
      claims
    }).recoverWith {
      case e: InvalidClaimException =>
        logger.error(s"invalid_claim=${e.getMessage}", e)
        Failure(e)
      case e: Exception =>
        logger.error(s"invalid_token=${e.getMessage}", e)
        Failure(e)
    }

  }

}
