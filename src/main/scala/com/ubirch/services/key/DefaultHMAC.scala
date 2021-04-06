package com.ubirch.services.key

import java.nio.charset.StandardCharsets

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import java.util.{ Base64, Date }

import org.bouncycastle.util.Arrays

import javax.crypto.{ Mac, SecretKey }
import javax.inject.Singleton

trait HMAC {
  def getHMAC(data: Array[Byte], time: Date, macKey: SecretKey): String
  def getHMAC(data: Array[Byte], macKey: SecretKey): String
}

@Singleton
class DefaultHMAC extends HMAC {

  Security.addProvider(new BouncyCastleProvider())

  override def getHMAC(data: Array[Byte], time: Date, macKey: SecretKey): String = {
    val fullData = Arrays.concatenate(data, time.getTime.toString.getBytes(StandardCharsets.UTF_8))
    getHMAC(fullData, macKey)
  }

  override def getHMAC(data: Array[Byte], macKey: SecretKey): String = {
    val mac = Mac.getInstance("HmacSHA256", "BC")
    mac.init(macKey)
    mac.update(data)
    Base64.getEncoder.encodeToString(mac.doFinal())
  }

}
