package com.ubirch.services.jwt

import java.time.Clock
import java.util.UUID

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.crypto.PrivKey
import com.ubirch.models.TokenClaim
import com.ubirch.util.TaskHelpers
import javax.inject.{ Inject, Singleton }
import monix.eval.Task
import pdi.jwt.{ Jwt, JwtAlgorithm, JwtClaim }

import scala.util.Try

trait TokenCreation {
  def create[T <: Any](
      by: String,
      to: String,
      about: String,
      expiresIn: Option[Long],
      notBefore: Option[Long],
      fields: (Symbol, T)*
  ): Try[JwtClaim]
  def encode(jwtClaim: JwtClaim, privKey: PrivKey): Try[String]
  def encode(tokenClaim: TokenClaim, privKey: PrivKey): Try[String]
  def encode(tokenClaim: TokenClaim): Task[String]
}

@Singleton
class DefaultTokenCreation @Inject() (config: Config) extends TokenCreation with TaskHelpers with LazyLogging {

  implicit private val clock: Clock = Clock.systemUTC

  private val privKeyInHex = config.getString("tokenSystem.tokenGen.privKeyInHex")

  override def create[T <: Any](
      by: String,
      to: String,
      about: String,
      expiresIn: Option[Long],
      notBefore: Option[Long],
      fields: (Symbol, T)*
  ): Try[JwtClaim] = {

    for {
      jwtClaim <- Try {
        JwtClaim()
          .by(by)
          .to(to)
          .about(about)
          .issuedNow
          .withId(UUID.randomUUID().toString)
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

  override def encode(tokenClaim: TokenClaim, privKey: PrivKey): Try[String] = {

    for {
      claims <- create(
        by = tokenClaim.issuer,
        to = tokenClaim.audience,
        about = tokenClaim.subject,
        expiresIn = tokenClaim.expiration,
        notBefore = tokenClaim.notBefore,
        fields = tokenClaim.content.toList: _*
      )

      jwtClaim <- encode(claims, privKey)

    } yield {
      jwtClaim
    }

  }

  override def encode(tokenClaim: TokenClaim): Task[String] = {

    //    for {
    //      privKey <- lift(GeneratorKeyFactory.getPrivKey(privKeyInHex, Curve.PRIME256V1))
    //      jwtClaim <- liftTry(encode(tokenClaim, privKey))
    //    } yield {
    //      jwtClaim
    //    }

    ???

  }

}
