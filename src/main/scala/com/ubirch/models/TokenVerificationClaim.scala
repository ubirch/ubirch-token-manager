package com.ubirch.models

import java.util.UUID

case class TokenVerificationClaim(
    tenantId: UUID,
    purpose: String,
    targetIdentities: Either[List[UUID], String],
    expiration: Option[Long],
    notBefore: Option[Long]
) {
  def validateIdentities: Boolean = {
    targetIdentities match {
      case Left(value) => value.nonEmpty
      case Right(value) => value == "*"
    }
  }

  def validatePurpose: Boolean = purpose.nonEmpty && purpose.length > 5
}

