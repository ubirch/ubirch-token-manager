package com.ubirch.models

import java.util.{ Date, UUID }

case class PlatformAccessToken(ownerId: UUID, value: String, tokenHashValue: String, createdAt: Date, expireAt: Date) {
  def asRow: PlatformAccessTokenRow = PlatformAccessTokenRow(ownerId, tokenHashValue, createdAt, expireAt)
}

case class PlatformAccessTokenRow(ownerId: UUID, tokenHashValue: String, createdAt: Date, expireAt: Date)

case class PlatformAccessTokenRequest(ownerId: UUID, validityDurationInDays: Option[Int])

case class PlatformAccessTokenDeleteRequest(ownerId: UUID, tokenHashValue: String)
