package com.ubirch.models

import java.util.UUID

case class TokenVerificationClaim(
    tenantId: UUID,
    purpose: String,
    targetIdentities: List[UUID],
    expiration: Option[Long],
    notBefore: Option[Long]
)

