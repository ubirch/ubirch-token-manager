package com.ubirch.services.key

import java.nio.charset.StandardCharsets
import java.util.Date

import com.ubirch.models.VerificationRequest
import com.ubirch.services.state.SecretKeyPoolService

import javax.crypto.SecretKey
import javax.inject.{ Inject, Singleton }

trait HMACVerifier {
  def verify(verificationRequest: VerificationRequest): Boolean
}

@Singleton
class DefaultHMACVerifier @Inject() (secretKeyPoolService: SecretKeyPoolService, HMAC: HMAC) extends HMACVerifier {

  def verify(verificationRequest: VerificationRequest): Boolean = {
    secretKeyPoolService.getKey(verificationRequest.sigPointer)
      .map { key =>
        HMAC.getHMAC(
          verificationRequest.signed.getOrElse("").getBytes(StandardCharsets.UTF_8),
          verificationRequest.time.map(x => new Date(x.toLong)).getOrElse(new Date()),
          key.asInstanceOf[SecretKey]
        )
      }.contains(verificationRequest.signature)

  }

}
