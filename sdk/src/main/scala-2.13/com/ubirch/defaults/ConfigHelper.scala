package com.ubirch.defaults

import com.typesafe.config.Config
import scala.jdk.CollectionConverters._

object ConfigHelper {
  def getStringList(config: Config, path: String): List[String] = {
    config.getStringList(path).asScala.toList
  }
}
