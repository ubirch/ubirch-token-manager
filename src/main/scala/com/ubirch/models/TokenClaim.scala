package com.ubirch.models

import java.util.UUID

import scala.collection.immutable.ListMap

case class Content(role: Symbol, env: Symbol)

case class TokenClaim(
    ownerId: UUID,
    issuer: String,
    subject: String,
    audience: String,
    expiration: Option[Long],
    notBefore: Option[Long],
    issuedAt: Option[Long],
    content: ListMap[Symbol, String]
) {
}

