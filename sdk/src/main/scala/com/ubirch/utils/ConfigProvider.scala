package com.ubirch.utils

import javax.inject._

import com.typesafe.config.{Config, ConfigFactory}

/**
  * Configuration Provider for the Configuration Component.
  */
@Singleton
class ConfigProvider extends Provider[Config] {

  val default = ConfigFactory.load()

  def conf: Config = default

  override def get(): Config = conf

}
