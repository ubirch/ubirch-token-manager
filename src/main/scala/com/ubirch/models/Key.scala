package com.ubirch.models

import java.util.{ Base64, UUID }

import com.ubirch.crypto.{ GeneratorKeyFactory, PubKey }
import com.ubirch.crypto.utils.Curve
import com.ubirch.util.PublicKeyUtil

import scala.util.Try

case class Key(algorithm: String, hwDeviceId: UUID, pubKey: String, pubKeyId: String) {
  def getCurve: Try[Curve] = PublicKeyUtil.associateCurve(algorithm)
  def getPrivKey: Try[PubKey] = {
    for {
      curve <- getCurve
      pubkey <- Try(GeneratorKeyFactory.getPubKey(Base64.getDecoder.decode(pubKey), curve))
    } yield pubkey
  }
}
