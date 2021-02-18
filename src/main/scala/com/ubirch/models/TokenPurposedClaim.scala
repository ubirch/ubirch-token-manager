package com.ubirch.models

import java.net.URL
import java.util.UUID

import com.ubirch.util.URLsHelper

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

  def hasMaybeGroups: Boolean = {
    targetIdentities match {
      case Left(_) => false
      case Right(List("*")) => false
      case Right(_) => true
    }
  }

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

  def toTokenClaim(ENV: String): TokenClaim = {
    val purposeKey = 'purpose -> purpose
    val targetIdentitiesKey = targetIdentities match {
      case Left(uuids) => 'target_identities -> uuids.distinct.map(_.toString).asInstanceOf[Any]
      case Right(other) => 'target_identities -> other.asInstanceOf[Any]
    }
    val originKey = 'origin_domains -> originDomains.distinct.map(_.toString)
    val typedScopes = scopes.sorted.flatMap(x => Scopes.fromString(x)).distinct
    val scopesKey = 'scopes -> typedScopes.map(Scopes.asString)

    TokenClaim(
      ownerId = tenantId,
      issuer = s"https://token.$ENV.ubirch.com",
      subject = tenantId.toString,
      audience = typedScopes.flatMap(x => Scopes.audience(x, ENV).toList).map(_.toString),
      expiration = expiration,
      notBefore = notBefore,
      issuedAt = None,
      content = Map(purposeKey, targetIdentitiesKey, originKey, scopesKey)
    )
  }

}

