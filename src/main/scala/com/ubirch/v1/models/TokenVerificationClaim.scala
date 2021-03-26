package com.ubirch.v1.models

import com.ubirch.v1.util.URLsHelper

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

  def validateOriginsDomains: Boolean = {
    (for {
      fts <- Try(originDomains).map(_.map(_.toURI))
      verifications <- Try(fts.map(x => URLsHelper.urlValidator().isValid(x.toString)))
    } yield {

      verifications.forall(b => b)
    }).getOrElse(false)
  }

  def validatePurpose: Boolean = purpose.nonEmpty && purpose.length > 5

}

