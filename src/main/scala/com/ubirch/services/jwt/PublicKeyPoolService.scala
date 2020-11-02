package com.ubirch.services.jwt

import java.security.Key

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.ConfPaths.TokenVerificationPaths
import javax.inject._
import monix.eval.Task
import monix.execution.Scheduler

import scala.collection.concurrent.TrieMap

trait PublicKeyPoolService {
  def getKey(kid: String): Option[Key]
  def getDefaultKey: Option[Key]
  def init: Task[List[(String, Key)]]
}

@Singleton
class DefaultPublicKeyPoolService @Inject() (config: Config, publicKeyDiscoveryService: PublicKeyDiscoveryService)(implicit scheduler: Scheduler) extends PublicKeyPoolService with LazyLogging {

  final val acceptedKids = List(config.getString(TokenVerificationPaths.KID))

  private final val cache = new TrieMap[String, Key]()

  override def getKey(kid: String): Option[Key] = cache.find(_._1 == kid).map(_._2)

  override def getDefaultKey: Option[Key] = acceptedKids.headOption.flatMap(x => getKey(x))

  def getKeyFromDiscoveryService(kid: String): Task[Option[Key]] = publicKeyDiscoveryService.getKey(kid)

  override def init: Task[List[(String, Key)]] = {
    Task.sequence {
      acceptedKids.map { kid =>
        for {
          maybeKey <- getKeyFromDiscoveryService(kid)
        } yield {

          val res = maybeKey match {
            case Some(value) => cache.put(kid, value).map(x => (kid, x))
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
