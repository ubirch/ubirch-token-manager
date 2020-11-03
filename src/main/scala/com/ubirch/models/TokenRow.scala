package com.ubirch.models

import java.util.{ Date, UUID }

case class TokenRow(id: UUID, ownerId: UUID, tokenValue: String, category: String, createdAt: Date)

