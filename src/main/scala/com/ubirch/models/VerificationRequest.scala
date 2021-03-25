package com.ubirch.models

import java.util.UUID

case class VerificationRequest(token: String, identity: UUID, signed: Option[String], signatureRaw: Option[String], time: Option[String]) {
  def sigParts: Array[String] = signatureRaw.getOrElse("").split("-", 2)
  def sigPointer: String = sigParts.headOption.getOrElse("")
  def signature: String = sigParts.tail.headOption.getOrElse("")
}
