package com.ubirch.utils

import com.typesafe.config.Config
import scala.jdk.CollectionConverters._

object ConfigHelper {
  def getStringList(config: Config, path: String): List[String] = {
    config.getStringList(Paths.VALID_AUDIENCE_PATH).asScala
  }
}
