package com.ubirch.models

import java.util.UUID

case class VerificationRequest(token: String, identity: UUID, signature: Option[String], time: Option[String])
