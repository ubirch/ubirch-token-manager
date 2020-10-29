package com.ubirch.controllers.concerns

import java.security.PublicKey

import com.ubirch.services.jwt.{ PublicKeyPoolService, TokenVerificationService }
import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }
import org.scalatra.ScalatraBase

class KeycloakBearerAuthStrategy(
    protected override val app: ScalatraBase,
    tokenVerificationService: TokenVerificationService,
    publicKeyPoolService: PublicKeyPoolService
) extends BearerAuthStrategy[Token](app) {

  override def name: String = "Keycloak Strategy"

  override protected def validate(token: String)(implicit request: HttpServletRequest, response: HttpServletResponse): Option[Token] = {
    val res = for {
      key <- publicKeyPoolService.getDefaultKey
      claims <- tokenVerificationService.decodeAndVerify(token, key.asInstanceOf[PublicKey])
    } yield {
      claims
    }

    res.map(x => Token(token, x))

  }
}

trait KeycloakBearerAuthenticationSupport extends BearerAuthenticationSupport {

  self: ScalatraBase =>

  override protected def registerAuthStrategies(): Unit = {
    scentry.register("Bearer", app => createStrategy(app))
  }

  protected def createStrategy(app: ScalatraBase): KeycloakBearerAuthStrategy

}

