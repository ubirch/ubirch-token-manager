package com.ubirch.defaults

import javax.inject._
import com.typesafe.config.Config
import com.ubirch.api.TokenPublicKey
import com.ubirch.crypto.utils.Curve
import com.ubirch.crypto.{ GeneratorKeyFactory, PubKey }

@Singleton
class DefaultTokenPublicKey @Inject() (config: Config) extends TokenPublicKey {

  private val publicKeyAsHex = config.getString(Paths.TOKEN_PUBLIC_KEY_PATH)
  override val pubKey: PubKey = GeneratorKeyFactory.getPubKey(publicKeyAsHex, Curve.PRIME256V1)

}
