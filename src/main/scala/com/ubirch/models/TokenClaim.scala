package com.ubirch.models

import java.util.UUID

case class Content(role: Symbol, env: Symbol)

case class TokenClaim(
    ownerId: UUID,
    issuer: String,
    subject: String,
    audience: String,
    expiration: Option[Long],
    notBefore: Option[Long],
    issuedAt: Option[Long],
    content: Map[Symbol, String]
)

