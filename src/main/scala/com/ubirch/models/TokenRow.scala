package com.ubirch.models

import java.util.{ Date, UUID }

case class TokenRow(id: UUID, ownerId: UUID, tokenValue: String, createdAt: Date)

