package com.ubirch.v1.services.jwt

import javax.inject.{ Inject, Singleton }

import com.typesafe.config.Config
import com.ubirch.v1.ConfPaths.TokenGenPaths
import com.ubirch.crypto.{ GeneratorKeyFactory, PrivKey }
import com.ubirch.crypto.utils.Curve
import org.jose4j.jwk.PublicJsonWebKey

trait TokenKeyService {
  val key: PrivKey
  val publicJWK: PublicJsonWebKey
}

@Singleton
class DefaultTokenKeyService @Inject() (config: Config) extends TokenKeyService {
  override final val key = GeneratorKeyFactory.getPrivKey(config.getString(TokenGenPaths.PRIV_KEY_IN_HEX), Curve.PRIME256V1)
  override final val publicJWK: PublicJsonWebKey = PublicJsonWebKey.Factory.newPublicJwk(key.getPublicKey)
}
