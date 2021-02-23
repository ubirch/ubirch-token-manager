package com.ubirch.models

import java.util.UUID

case class VerificationRequest(token: String, identity: UUID, signed: Option[String], signature: Option[String], time: Option[String]) {
  def sigParts: Array[String] = signature.getOrElse("").split("-", 2)
  def sigPointer: String = sigParts.headOption.getOrElse("")
  def sig: String = sigParts.tail.headOption.getOrElse("")
}
