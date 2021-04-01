package com.ubirch.models

import java.nio.charset.StandardCharsets
import java.util.{ Base64, UUID }

case class BootstrapRequest(token: String, identity: UUID, signed: Option[String], signature: Option[String]) {
  def signedRaw: Array[Byte] = signed.getOrElse("").getBytes(StandardCharsets.UTF_8)
  def signatureRaw: Array[Byte] = Base64.getDecoder.decode(signature.getOrElse(""))
}
