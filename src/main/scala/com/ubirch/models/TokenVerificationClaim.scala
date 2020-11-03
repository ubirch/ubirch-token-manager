package com.ubirch.models

import java.util.UUID

case class TokenVerificationClaim(
    tenantId: UUID,
    purpose: String,
    target_identity: UUID,
    expiration: Option[Long],
    notBefore: Option[Long]
)

