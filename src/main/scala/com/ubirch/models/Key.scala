package com.ubirch.models

import java.util.UUID

case class Key(algorithm: String, hwDeviceId: UUID, pubKey: String, pubKeyId: String)
