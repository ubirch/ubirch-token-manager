package com.ubirch.controllers.concerns

import com.ubirch.models.NOK
import com.ubirch.services.key.HMACVerifier
import org.scalatra.servlet.ServletApiImplicits.enrichRequest
import org.scalatra.{ AsyncResult, halt }

import java.util.Date
import javax.servlet.http.HttpServletRequest

case class HMACData(time: Date, signaturePair: String, data: Array[Byte])

trait HMACAuthenticationSupport {
  val hmacVerifier: HMACVerifier

  def hmacAuth()(action: HMACData => AsyncResult)(implicit request: HttpServletRequest): AsyncResult = {
    val reqTimestamp = Option(request.getHeader("X-Ubirch-Timestamp")).filter(_.nonEmpty)
    if (reqTimestamp.isEmpty) halt(401, NOK.authenticationError("X-Ubirch-Timestamp header is required"))

    val reqSig = Option(request.getHeader("X-Ubirch-Signature")).filter(_.nonEmpty)
    if (reqSig.isEmpty) halt(401, NOK.authenticationError("X-Ubirch-Signature header is required"))

    val hmacData = HMACData(new Date(reqTimestamp.get.toLong), reqSig.get, request.body.getBytes)
    if (hmacVerifier.verify(hmacData.time, hmacData.signaturePair, hmacData.data)) action(hmacData) else halt(401, NOK.authenticationError("Invalid Request Signature, The hmac verification failed"))
  }
}
