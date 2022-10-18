package com.ubirch.defaults

import java.util.UUID

import com.ubirch.api.{
  Claims,
  ExternalStateVerifier,
  InvalidClaimException,
  TokenManager,
  TokenVerification,
  VerificationRequest
}
import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.annotations.{ UnsafeBecauseBlocking, UnsafeBecauseImpure }
import monix.execution.schedulers.CanBlock

import scala.concurrent.duration.Duration
import scala.util.{ Failure, Try }

abstract class TokenManagerImpl extends TokenManager {

  val tokenVerification: TokenVerification
  val externalStateVerifier: ExternalStateVerifier

  override def getClaims(token: String): Try[Claims] = token.split(" ").toList match {
    case List(x, y) =>
      val isBearer = x.toLowerCase == "bearer"
      val claims = tokenVerification.decodeAndVerify(y)
      if (isBearer && claims.isSuccess) claims
      else Failure(InvalidClaimException("Invalid Check", "Either is not Bearer or extraction failed."))
    case _ =>
      Failure(InvalidClaimException("Invalid Elements", "The token definition seems not to have the required parts"))
  }

  override def decodeAndVerify(jwt: String): Try[Claims] = tokenVerification.decodeAndVerify(jwt)

  override def externalStateVerify(accessToken: String, identity: UUID): Task[Boolean] =
    externalStateVerifier.verify(VerificationRequest(accessToken, identity))

  @UnsafeBecauseImpure
  @UnsafeBecauseBlocking
  override def externalStateVerifySync(accessToken: String, identity: UUID)(timeout: Duration)(
    implicit s: Scheduler,
    permit: CanBlock): Either[Throwable, Boolean] =
    externalStateVerify(accessToken, identity).attempt.runSyncUnsafe(timeout)

}
