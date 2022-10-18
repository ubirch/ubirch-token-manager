package com.ubirch.defaults

import java.nio.charset.StandardCharsets

import com.ubirch.api.{ ExternalResponseData, ExternalStateGetter, ExternalStateVerifier, VerificationRequest }
import monix.eval.Task

import javax.inject.{ Inject, Singleton }
import com.typesafe.scalalogging.LazyLogging
import org.json4s.JsonAST.{ JBool, JField, JObject }

@Singleton
class DefaultStateVerifier @Inject() (
  externalStateGetter: ExternalStateGetter,
  jsonConverterService: DefaultJsonConverterService)
  extends ExternalStateVerifier
  with LazyLogging {

  def isResponseOK(bodyAsString: String): Task[Boolean] = {
    for {
      resBodyJValue <- Task.fromEither(jsonConverterService.toJValue(bodyAsString))
      isOK <- Task.delay {
        for {
          JObject(obj) <- resBodyJValue
          JField("ok", JBool(ok)) <- obj
          JField("data", JBool(data)) <- obj
        } yield ok && data
      }.map(_.headOption.getOrElse(false))
    } yield isOK
  }

  def verifyExternalClaims(verificationRequest: VerificationRequest): Task[ExternalResponseData[String]] = {
    for {
      data <- Task.fromEither(jsonConverterService.toString[VerificationRequest](verificationRequest))
      res <- Task.delay(externalStateGetter.verify(data.getBytes(StandardCharsets.UTF_8))).map { x =>
        x.copy(body = new String(x.body))
      }
    } yield res
  }

  override def verify(verificationRequest: VerificationRequest): Task[Boolean] = {
    for {
      res <- verifyExternalClaims(verificationRequest)
      isOK <- isResponseOK(res.body).map(_ && res.status <= 299)
      _ = logger.info(
        "identity:" + verificationRequest.identity +
          " res_status:" + res.status +
          " res_body:" + res.body +
          " verification:" + isOK
      )
    } yield isOK
  }

}
