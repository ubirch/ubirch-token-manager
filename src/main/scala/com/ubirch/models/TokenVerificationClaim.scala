package com.ubirch.models

import java.net.URL
import java.util.UUID
import scala.util.Try

case class TokenVerificationClaim(
    tenantId: UUID,
    purpose: String,
    targetIdentities: Either[List[UUID], String],
    expiration: Option[Long],
    notBefore: Option[Long],
    originDomains: List[URL]
) {
  def validateIdentities: Boolean = {
    targetIdentities match {
      case Left(value) => value.nonEmpty
      case Right(value) => value == "*"
    }
  }

  def validateOriginsDomains: Boolean = Try(originDomains).map(_.map(_.toURI)).isSuccess

  def validatePurpose: Boolean = purpose.nonEmpty && purpose.length > 5
}

