package com.ubirch.defaults

import com.typesafe.config.Config
import scala.collection.JavaConverters._

object ConfigHelper {
  def getStringList(config: Config, path: String): List[String] = {
    config.getStringList(path).asScala.toList
  }
}
