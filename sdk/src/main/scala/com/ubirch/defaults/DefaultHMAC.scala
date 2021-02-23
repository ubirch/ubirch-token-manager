package com.ubirch.defaults

import java.nio.charset.StandardCharsets

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import java.util.{ Base64, Date }

import com.typesafe.config.Config
import com.ubirch.api.HMAC
import org.bouncycastle.util.Arrays

import javax.crypto.{ Mac, SecretKey }
import javax.crypto.spec.SecretKeySpec
import javax.inject.{ Inject, Singleton }

@Singleton
class DefaultHMAC @Inject() (config: Config) extends HMAC {

  Security.addProvider(new BouncyCastleProvider())

  private final val SECRET: String = config.getString(Paths.SECRET_PATH)
  private final val MAC_KEY = new SecretKeySpec(Base64.getDecoder.decode(SECRET), "HmacSHA256")

  override def getHMAC(data: Array[Byte]): String = getHMAC(data, MAC_KEY)

  override def getHMAC(data: Array[Byte], time: Date): String = {
    val fullData = Arrays.concatenate(data, time.getTime.toString.getBytes(StandardCharsets.UTF_8))
    getHMAC(fullData)
  }

  def getHMAC(data: Array[Byte], macKey: SecretKey): String = {
    val mac = Mac.getInstance("HmacSHA256", "BC")
    mac.init(macKey)
    mac.update(data)
    Base64.getEncoder.encodeToString(mac.doFinal())
  }

}
