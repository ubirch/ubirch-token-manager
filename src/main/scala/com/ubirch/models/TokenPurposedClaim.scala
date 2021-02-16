package com.ubirch.models

import com.ubirch.util.URLsHelper

import java.net.URL
import java.util.UUID
import scala.util.Try

case class TokenPurposedClaim(
    tenantId: UUID,
    purpose: String,
    targetIdentities: Either[List[UUID], List[String]],
    expiration: Option[Long],
    notBefore: Option[Long],
    originDomains: List[URL],
    scopes: List[String]
) {

  def validateIdentities: Boolean = {
    targetIdentities match {
      case Left(value) => value.nonEmpty
      case Right(List("*")) => true
      case Right(_) => false
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

  def validateScopes: Boolean = scopes match {
    case Nil => false
    case _ => scopes.flatMap(x => Scopes.fromString(x)).distinct.map(Scopes.asString).nonEmpty
  }

}

