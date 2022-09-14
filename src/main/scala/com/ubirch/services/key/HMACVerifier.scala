package com.ubirch.services.key

import java.nio.charset.StandardCharsets
import java.util.Date

import com.ubirch.models.VerificationRequest
import com.ubirch.services.state.SecretKeyPoolService
import org.joda.time.{ DateTime, Duration }

import javax.crypto.SecretKey
import javax.inject.{ Inject, Singleton }

trait HMACVerifier {
  def verify(verificationRequest: VerificationRequest): Boolean

  def verify(time: Date, signaturePair: String, data: Array[Byte]): Boolean
}

@Singleton
class DefaultHMACVerifier @Inject() (secretKeyPoolService: SecretKeyPoolService, HMAC: HMAC) extends HMACVerifier {

  def verify(verificationRequest: VerificationRequest): Boolean = {
    (for {
      reqTime <- verificationRequest.time.map(x => new Date(x.toLong))
      secsInBetween <- Option(new Duration(new DateTime(reqTime), new DateTime()).abs().getStandardSeconds)
      signed <- verificationRequest.signed.map(_.getBytes(StandardCharsets.UTF_8))
      key <- secretKeyPoolService.getKey(verificationRequest.sigPointer)
    } yield {
      (HMAC.getHMAC(
        signed,
        reqTime,
        key.asInstanceOf[SecretKey]
      ) == verificationRequest.signature) && secsInBetween < 120
    }).getOrElse(false)
  }

  def verify(time: Date, signaturePair: String, data: Array[Byte]): Boolean = {
    val secsInBetween = new Duration(new DateTime(time), new DateTime()).abs().getStandardSeconds
    val sigPointer = signaturePair.split("-", 2).headOption.getOrElse("")
    val signature = signaturePair.split("-", 2).tail.headOption.getOrElse("")
    val keyOpt = secretKeyPoolService.getKey(sigPointer)

    keyOpt.exists { key =>
      (HMAC.getHMAC(data, time, key.asInstanceOf[SecretKey]) == signature) && secsInBetween < 1230000
    }
  }

}
