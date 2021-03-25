package com.ubirch.services.key

import java.security.Key

import scala.collection.concurrent.TrieMap

trait KeyPoolService {
  def getKey(kid: String): Option[Key]
  def addKey(kid: String, key: Key): Option[Key]
}

class DefaultKeyPoolService extends KeyPoolService {

  private final val cache = new TrieMap[String, Key]()

  override def getKey(kid: String): Option[Key] = cache.find(_._1 == kid).map(_._2)
  override def addKey(kid: String, key: Key): Option[Key] = cache.put(kid, key)

}
