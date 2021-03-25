package com.ubirch.services.jwt

import java.security.Key

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.ConfPaths.TokenVerificationPaths
import monix.eval.Task

import javax.inject._

import com.ubirch.services.key.KeyPoolService

trait PublicKeyPoolService {
  def getKey(kid: String): Option[Key]
  def getDefaultKey: Option[Key]
  def init: Task[List[(String, Key)]]
}

@Singleton
class DefaultPublicKeyPoolService @Inject() (config: Config, publicKeyDiscoveryService: PublicKeyDiscoveryService, keyPoolService: KeyPoolService) extends PublicKeyPoolService with LazyLogging {

  final val acceptedKids = List(config.getString(TokenVerificationPaths.KID))

  override def getKey(kid: String): Option[Key] = keyPoolService.getKey(kid)

  override def getDefaultKey: Option[Key] = acceptedKids.headOption.flatMap(x => getKey(x))

  def getKeyFromDiscoveryService(kid: String): Task[Option[Key]] = publicKeyDiscoveryService.getKey(kid)

  override def init: Task[List[(String, Key)]] = {
    Task.sequence {
      acceptedKids.map { kid =>
        for {
          maybeKey <- getKeyFromDiscoveryService(kid)
        } yield {

          val res = maybeKey match {
            case Some(value) => keyPoolService.addKey(kid, value).map(x => (kid, x))
            case None =>
              logger.warn("kid_not_found={}", kid)
              None
          }
          res.toList

        }
      }
    }.map(_.flatten)

  }

}
