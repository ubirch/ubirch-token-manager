package com.ubirch.defaults

import com.google.inject.AbstractModule
import com.typesafe.config.Config
import com.ubirch.api._
import com.ubirch.utils.{ ConfigProvider, InjectorHelper }
import org.json4s.Formats

import scala.util.{ Failure, Try }

object TokenApi extends TokenManager {

  final private val injector: InjectorHelper = new InjectorHelper(List(new AbstractModule {
    override def configure(): Unit = {
      bind(classOf[JsonConverterService]).to(classOf[DefaultJsonConverterService])
      bind(classOf[TokenPublicKey]).to(classOf[DefaultTokenPublicKey])
      bind(classOf[TokenVerification]).to(classOf[DefaultTokenVerification])

      bind(classOf[Config]).toProvider(classOf[ConfigProvider])
      bind(classOf[Formats]).toProvider(classOf[JsonFormatsProvider])
      ()
    }
  })) {}

  final private val tokenVerification: TokenVerification = injector.get[TokenVerification]

  override def getClaims(token: String): Try[Claims] = token.split(" ").toList match {
    case List(x, y) =>
      val isBearer = x.toLowerCase == "bearer"
      val claims = tokenVerification.decodeAndVerify(y)
      if (isBearer && claims.isSuccess) claims
      else Failure(InvalidToken("Invalid Check", "Either is not Bearer or extraction failed."))
    case _ => Failure(InvalidToken("Invalid Elements", "The token definition seems not to have the required parts"))
  }

  override def decodeAndVerify(jwt: String): Try[Claims] = tokenVerification.decodeAndVerify(jwt)

}
