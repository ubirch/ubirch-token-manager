package com.ubirch.models

import java.util.UUID

import pdi.jwt.JwtClaim

case class TokenCreationData(id: UUID, jwtClaim: JwtClaim, token: String)
