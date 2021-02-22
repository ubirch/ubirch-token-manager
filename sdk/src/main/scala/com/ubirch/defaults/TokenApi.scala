package com.ubirch.defaults

import java.util.UUID

import com.ubirch.api._
import monix.eval.Task

import scala.util.{ Failure, Try }

object TokenApi extends TokenManager with Binding {

  final private val tokenVerification: TokenVerification = injector.get[TokenVerification]
  final private val externalStateVerifier: ExternalStateVerifier = injector.get[ExternalStateVerifier]

  override def getClaims(token: String): Try[Claims] = token.split(" ").toList match {
    case List(x, y) =>
      val isBearer = x.toLowerCase == "bearer"
      val claims = tokenVerification.decodeAndVerify(y)
      if (isBearer && claims.isSuccess) claims
      else Failure(InvalidClaimException("Invalid Check", "Either is not Bearer or extraction failed."))
    case _ => Failure(InvalidClaimException("Invalid Elements", "The token definition seems not to have the required parts"))
  }

  override def decodeAndVerify(jwt: String): Try[Claims] = tokenVerification.decodeAndVerify(jwt)
  override def externalStateVerify(accessToken: String, identity: UUID): Task[Boolean] = externalStateVerifier.verify(VerificationRequest(accessToken, identity))

}
