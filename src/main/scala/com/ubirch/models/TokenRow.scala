package com.ubirch.models

import java.time.Instant
import java.util.UUID

case class TokenRow(id: UUID, ownerId: UUID, tokenValue: String, category: String, createdAt: Instant)

