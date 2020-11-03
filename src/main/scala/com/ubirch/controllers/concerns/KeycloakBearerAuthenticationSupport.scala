package com.ubirch.controllers.concerns

import java.security.PublicKey

import com.ubirch.services.jwt.{ PublicKeyPoolService, TokenVerificationService }
import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }
import org.json4s.JsonAST
import org.scalatra.ScalatraBase

class KeycloakBearerAuthStrategy(
    protected override val app: ScalatraBase,
    tokenVerificationService: TokenVerificationService,
    publicKeyPoolService: PublicKeyPoolService
) extends BearerAuthStrategy[Token](app) {

  override def name: String = "Keycloak Strategy"

  override protected def validate(token: String)(implicit request: HttpServletRequest, response: HttpServletResponse): Option[Token] = {
    for {
      key <- publicKeyPoolService.getDefaultKey

      claims <- tokenVerificationService.decodeAndVerify(token, key.asInstanceOf[PublicKey])

      sub <- claims.findField(_._1 == "sub").map(_._2).collect {
        case JsonAST.JString(s) => s
        case _ => ""
      }

      name <- claims.findField(_._1 == "name").map(_._2).collect {
        case JsonAST.JString(s) => s
        case _ => ""
      }

      email <- claims.findField(_._1 == "email").map(_._2).collect {
        case JsonAST.JString(s) => s
        case _ => ""
      }

      roles <- claims
        .findField(_._1 == "realm_access")
        .map(_._2)
        .flatMap(_.findField(_._1 == "roles"))
        .map(_._2)
        .collect {
          case JsonAST.JArray(arr) =>
            arr.collect {
              case JsonAST.JString(s) => s
              case _ => ""
            }
              .filter(_.nonEmpty)
              .map(Symbol(_))
          case _ => Nil
        }

      t = Token(token, claims, sub, name, email, roles)
      if t.isUser || t.isAdmin

    } yield {
      t
    }

  }

}

trait KeycloakBearerAuthenticationSupport extends BearerAuthenticationSupport {

  self: ScalatraBase =>

  override protected def registerAuthStrategies(): Unit = {
    scentry.register("Bearer", app => createStrategy(app))
  }

  protected def createStrategy(app: ScalatraBase): KeycloakBearerAuthStrategy

}

