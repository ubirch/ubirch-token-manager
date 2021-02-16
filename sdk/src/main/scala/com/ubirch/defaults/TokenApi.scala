package com.ubirch.defaults

import com.google.inject.AbstractModule
import com.typesafe.config.Config
import com.ubirch.api._
import com.ubirch.utils.{ ConfigProvider, InjectorHelper }
import org.json4s.Formats

object TokenApi extends TokenManager {

  final private val injector: InjectorHelper = new InjectorHelper(List(new AbstractModule {
    override def configure(): Unit = {
      bind(classOf[JsonConverterService]).to(classOf[DefaultJsonConverterService])
      bind(classOf[TokenPublicKey]).to(classOf[DefaultTokenPublicKey])
      bind(classOf[TokenVerification]).to(classOf[DefaultTokenVerification])

      bind(classOf[Config]).toProvider(classOf[ConfigProvider])
      bind(classOf[Formats]).toProvider(classOf[JsonFormatsProvider])
    }
  })) {}

  override val tokenVerification: TokenVerification = injector.get[TokenVerification]

}
