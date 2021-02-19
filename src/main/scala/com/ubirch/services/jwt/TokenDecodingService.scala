package com.ubirch
package services.jwt

import java.security.PublicKey

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.services.formats.JsonConverterService
import org.json4s.JValue
import pdi.jwt.{ Jwt, JwtAlgorithm }

import javax.inject.{ Inject, Singleton }
import scala.util.{ Failure, Try }

trait TokenDecodingService {
  def decodeAndVerify(jwt: String, publicKey: PublicKey): Try[JValue]
}

@Singleton
class DefaultTokenDecodingService @Inject() (jsonConverterService: JsonConverterService) extends TokenDecodingService with LazyLogging {

  def decodeAndVerify(jwt: String, publicKey: PublicKey): Try[JValue] = {
    (for {
      (_, p, _) <- Jwt.decodeRawAll(jwt, publicKey, Seq(JwtAlgorithm.ES256))

      all <- jsonConverterService.toJValue(p).toTry
        .recover { case e: Exception => throw InvalidAllClaims(e.getMessage, jwt) }

    } yield {
      all
    }).recoverWith {
      case e: InvalidAllClaims =>
        logger.error(s"invalid_token_all_claims=${e.getMessage}", e)
        Failure(e)
      case e: Exception =>
        logger.error(s"invalid_token=${e.getMessage}", e)
        Failure(e)
    }

  }

}
