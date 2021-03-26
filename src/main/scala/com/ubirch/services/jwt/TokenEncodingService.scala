package com.ubirch.services.jwt

import java.time.Clock
import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.crypto.PrivKey
import com.ubirch.models.TokenClaim
import com.ubirch.util.TaskHelpers
import pdi.jwt.{ Jwt, JwtAlgorithm, JwtClaim }

import javax.inject.Singleton
import scala.util.Try

trait TokenEncodingService {
  def create[T <: Any](
      id: UUID,
      by: String,
      to: Either[Set[String], String],
      about: String,
      expiresIn: Option[Long],
      notBefore: Option[Long],
      fields: (Symbol, T)*
  ): Try[JwtClaim]
  def encode(jwtClaim: JwtClaim, privKey: PrivKey): Try[String]
  def encode(id: UUID, tokenClaim: TokenClaim, privKey: PrivKey): Try[(String, JwtClaim)]
  def encode(header: String, jwtClaim: String, privKey: PrivKey): Try[String]
}

@Singleton
class DefaultTokenEncodingService extends TokenEncodingService with TaskHelpers with LazyLogging {

  implicit private val clock: Clock = Clock.systemUTC

  override def create[T <: Any](
      id: UUID,
      by: String,
      to: Either[Set[String], String],
      about: String,
      expiresIn: Option[Long],
      notBefore: Option[Long],
      fields: (Symbol, T)*
  ): Try[JwtClaim] = {

    for {
      jwtClaim <- Try {
        val jc = JwtClaim()
          .by(by)
          .about(about)
          .issuedNow
          .withId(id.toString)

        to match {
          case Left(aud) => jc.to(aud)
          case Right(aud) => jc.to(aud)
        }
      }
        .map { x => expiresIn.map(x.expiresIn(_)).getOrElse(x) }
        .map { x => notBefore.map(x.startsIn(_)).getOrElse(x) }

      jwtClaimWithFields = jwtClaim ++ (fields.map(x => (x._1.name, x._2)): _*)

    } yield {
      jwtClaimWithFields
    }

  }

  override def encode(jwtClaim: JwtClaim, privKey: PrivKey): Try[String] = Try {
    Jwt.encode(
      jwtClaim,
      privKey.getPrivateKey,
      JwtAlgorithm.ES256
    )
  }

  override def encode(header: String, jwtClaim: String, privKey: PrivKey): Try[String] = Try {
    Jwt.encode(
      header,
      jwtClaim,
      privKey.getPrivateKey,
      JwtAlgorithm.ES256
    )
  }

  override def encode(id: UUID, tokenClaim: TokenClaim, privKey: PrivKey): Try[(String, JwtClaim)] = {

    val aud: Either[Set[String], String] = tokenClaim.audience match {
      case List("") => Right("")
      case list => Left(list.toSet)
    }

    for {
      claims <- create(
        id = id,
        by = tokenClaim.issuer,
        to = aud,
        about = tokenClaim.subject,
        expiresIn = tokenClaim.expiration,
        notBefore = tokenClaim.notBefore,
        fields = tokenClaim.content.toList: _*
      )

      jwtClaim <- encode(claims, privKey)

    } yield {
      (jwtClaim, claims)
    }

  }

}
