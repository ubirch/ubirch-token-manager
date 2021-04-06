package com.ubirch.services.jwt

import com.typesafe.config.Config
import com.ubirch.ConfPaths.TokenGenPaths
import com.ubirch.crypto.utils.Curve
import com.ubirch.crypto.{ GeneratorKeyFactory, PrivKey }
import org.jose4j.jwk.PublicJsonWebKey

import javax.inject.{ Inject, Singleton }

trait TokenKeyService {
  val key: PrivKey
  val publicJWK: PublicJsonWebKey
}

@Singleton
class DefaultTokenKeyService @Inject() (config: Config) extends TokenKeyService {
  override final val key = GeneratorKeyFactory.getPrivKey(config.getString(TokenGenPaths.PRIV_KEY_IN_HEX), Curve.PRIME256V1)
  override final val publicJWK: PublicJsonWebKey = PublicJsonWebKey.Factory.newPublicJwk(key.getPublicKey)
}
