package com.ubirch.models

case class BootstrapToken(registration: TokenCreationData, anchoring: TokenCreationData, verification: TokenCreationData)
