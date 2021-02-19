package com.ubirch.defaults

import java.nio.charset.StandardCharsets

import com.ubirch.api.{ ExternalStateGetter, ExternalStateVerifier, VerificationRequest }
import monix.eval.Task
import javax.inject.{ Inject, Singleton }

import com.typesafe.scalalogging.LazyLogging
import org.json4s.JsonAST.{ JBool, JField, JObject }

@Singleton
class DefaultStateVerifier @Inject() (externalStateGetter: ExternalStateGetter, jsonConverterService: DefaultJsonConverterService) extends ExternalStateVerifier with LazyLogging {

  override def verify(verificationRequest: VerificationRequest): Task[Boolean] = {
    for {
      data <- Task.fromEither(jsonConverterService.toString[VerificationRequest](verificationRequest))
      res <- Task.delay(externalStateGetter.verify(data.getBytes(StandardCharsets.UTF_8)))

      resBody <- Task(new String(res.body))
      _ = logger.info("identity:" + verificationRequest.identity + " res_status:" + res.status + " res_body:" + resBody)

      resBodyJValue <- Task.fromEither(jsonConverterService.toJValue(resBody))

      isOK <- Task.delay {
        for {
          JObject(obj) <- resBodyJValue
          JField("ok", JBool(ok)) <- obj
          JField("data", JBool(data)) <- obj
        } yield ok && data
      }.map(_.headOption.getOrElse(false))

    } yield {
      if (res.status <= 299 && isOK) true else false
    }
  }
}
