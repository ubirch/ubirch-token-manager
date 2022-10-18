package com.ubirch.utils

import com.typesafe.config.Config
import scala.collection.JavaConverters._

object ConfigHelper {
  def getStringList(config: Config, path: String): List[String] = {
    config.getStringList(Paths.VALID_AUDIENCE_PATH).asScala
  }
}
