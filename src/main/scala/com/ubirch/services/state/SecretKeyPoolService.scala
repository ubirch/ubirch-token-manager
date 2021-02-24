package com.ubirch.services.state

import java.security.Key
import java.util.Base64

import com.ubirch.crypto.utils.Utils

import javax.crypto.spec.SecretKeySpec
import javax.inject.{ Inject, Singleton }
import com.ubirch.services.key.KeyPoolService
import monix.eval.Task

trait SecretKeyPoolService {
  def getKey(kid: String): Option[Key]
  def init: Task[List[(String, Key)]]
}

@Singleton
class DefaultSecretKeyPoolService @Inject() (tokenClientsInfo: TokenClientsInfo, keyPoolService: KeyPoolService) extends SecretKeyPoolService {

  override def getKey(kid: String): Option[Key] = keyPoolService.getKey(kid)

  def buildKey(secret: String): Key = new SecretKeySpec(Base64.getDecoder.decode(secret), "HmacSHA256")

  override def init: Task[List[(String, Key)]] = {
    for {
      clients <- Task.delay(tokenClientsInfo.info)
      decKeys <- Task.delay(clients.map(x => (x, buildKey(x.secretKey))))
      keys <- Task.delay(decKeys.flatMap { case (cl, k) =>
        keyPoolService.addKey(cl.secretPointer, k)
          .map(x => (cl.secretPointer, x))
      })
    } yield keys
  }

}

object DefaultSecretKeyPoolService {
  def main(args: Array[String]): Unit = {
    val key = Base64.getEncoder.encodeToString(Utils.secureRandomBytes(9)) + "-" + Base64.getEncoder.encodeToString(Utils.secureRandomBytes(33))
    println(key)

  }
}
