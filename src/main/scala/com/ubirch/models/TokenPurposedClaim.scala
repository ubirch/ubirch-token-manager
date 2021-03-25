package com.ubirch.models

import java.net.URL
import java.util.UUID

import com.ubirch.util.URLsHelper

import scala.util.Try

case class TokenPurposedClaim(
    tenantId: UUID,
    purpose: String,
    targetIdentities: Either[List[UUID], List[String]],
    targetGroups: Either[List[UUID], List[String]],
    expiration: Option[Long],
    notBefore: Option[Long],
    originDomains: List[URL],
    scopes: List[String]
) {

  def hasMaybeGroups: Boolean = {
    targetGroups.left
      .map(_.map(_.toString))
      .merge
      .exists(x => x.nonEmpty && x != "*")
  }

  def hasMaybeIdentities: Boolean = {
    targetIdentities.left
      .map(_.map(_.toString))
      .merge
      .exists(x => x.nonEmpty && x != "*")
  }

  def validateGroups: Boolean = {
    targetGroups match {
      case Left(value) => value.nonEmpty
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
    case _ => scopes.flatMap(x => Scope.fromString(x)).distinct.map(Scope.asString).nonEmpty
  }

  def toTokenClaim(ENV: String): TokenClaim = {
    val purposeKey = TokenPurposedClaim.PURPOSE_KEY -> purpose
    val targetIdentitiesKey = TokenPurposedClaim.TARGET_IDENTITIES_KEY -> targetIdentities.left.map(_.map(_.toString)).merge.distinct.asInstanceOf[Any]
    val targetGroupsKey = TokenPurposedClaim.TARGET_GROUPS_KEY -> targetGroups.left.map(_.map(_.toString)).merge.distinct.asInstanceOf[Any]
    val originKey = TokenPurposedClaim.ORIGIN_KEY -> originDomains.distinct.map(_.toString)
    val typedScopes = scopes.sorted.flatMap(x => Scope.fromString(x)).distinct
    val scopesKey = TokenPurposedClaim.SCOPES_KEY -> typedScopes.map(Scope.asString)

    TokenClaim(
      ownerId = tenantId,
      issuer = s"https://token.$ENV.ubirch.com",
      subject = tenantId.toString,
      audience = typedScopes.flatMap(x => Scope.audience(x, ENV).toList).map(_.toString),
      expiration = expiration,
      notBefore = notBefore,
      issuedAt = None,
      content = Map(purposeKey, targetIdentitiesKey, targetGroupsKey, originKey, scopesKey)
    )
  }

}

object TokenPurposedClaim {
  final val PURPOSE_KEY = 'pur
  final val TARGET_IDENTITIES_KEY = 'tid
  final val TARGET_GROUPS_KEY = 'tgp
  final val ORIGIN_KEY = 'ord
  final val SCOPES_KEY = 'scp
}

