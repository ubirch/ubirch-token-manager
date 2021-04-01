package com.ubirch.models

import java.nio.charset.StandardCharsets
import java.util.UUID

case class BootstrapRequest(token: String, identity: UUID, signed: Option[String], signature: Option[String]) {
  def signedRaw: Array[Byte] = signed.getOrElse("").getBytes(StandardCharsets.UTF_8)
  def signatureRaw: Array[Byte] = signature.getOrElse("").getBytes(StandardCharsets.UTF_8)
}
