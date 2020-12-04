package com.ubirch.controllers.concerns

import java.util.Locale

import com.ubirch.models.NOK
import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }
import org.json4s.JNothing
import org.json4s.JsonAST.JValue
import org.scalatra.ScalatraBase
import org.scalatra.auth.{ ScentryConfig, ScentryStrategy, ScentrySupport }

import scala.language.implicitConversions

trait BearerAuthSupport[TokenType <: AnyRef] {
  self: ScalatraBase with ScentrySupport[TokenType] =>

  def realm: String

  protected def bearerAuth()(implicit request: HttpServletRequest, response: HttpServletResponse): Option[TokenType] = {
    val beReq = new BearerAuthStrategy.BearerAuthRequest(request)
    if (!beReq.providesAuth) {
      response.setHeader("WWW-Authenticate", "Bearer realm=\"%s\"" format realm)
      halt(401, NOK.authenticationError("Unauthenticated"))
    }
    if (!beReq.isBearerAuth) {
      halt(400, NOK.authenticationError("Invalid bearer token"))
    }

    scentry.authenticate("Bearer")

  }

  protected def authenticated(p: TokenType => Boolean = _ => true)(action: TokenType => Any)(implicit request: HttpServletRequest, response: HttpServletResponse): Any = {
    bearerAuth() match {
      case Some(value) if p(value) => action(value)
      case _ => halt(403, NOK.authenticationError("Forbidden"))
    }
  }

}

object BearerAuthStrategy {

  implicit def request2BearerAuthRequest(r: HttpServletRequest): BearerAuthRequest = new BearerAuthRequest(r)

  private val AUTHORIZATION_KEYS = List("Authorization", "HTTP_AUTHORIZATION", "X-HTTP_AUTHORIZATION", "X_HTTP_AUTHORIZATION")

  class BearerAuthRequest(r: HttpServletRequest) {

    def parts: Seq[String] = authorizationKey map { r.getHeader(_).split(" ", 2).toList } getOrElse Nil
    def scheme: Option[String] = parts.headOption.map(sch => sch.toLowerCase(Locale.ENGLISH))
    def params: Option[String] = parts.lastOption

    private def authorizationKey: Option[String] = AUTHORIZATION_KEYS.find(r.getHeader(_) != null)

    def isBearerAuth: Boolean = (false /: scheme) { (_, sch) => sch == "bearer" }
    def providesAuth: Boolean = authorizationKey.isDefined

    private[this] val credentials = params.getOrElse("")
    def token: String = credentials
  }

}

abstract class BearerAuthStrategy[TokenType <: AnyRef](protected override val app: ScalatraBase) extends ScentryStrategy[TokenType] {

  import BearerAuthStrategy.request2BearerAuthRequest

  override def isValid(implicit request: HttpServletRequest): Boolean = request.isBearerAuth && request.providesAuth

  override def authenticate()(implicit request: HttpServletRequest, response: HttpServletResponse): Option[TokenType] = {
    validate(request.token)
  }

  protected def validate(token: String)(implicit request: HttpServletRequest, response: HttpServletResponse): Option[TokenType]

}

case class Token(value: String, json: JValue, sub: String, name: String, email: String, roles: List[Symbol]) {
  def id: String = sub
  def ownerId: String = id
  def isAdmin: Boolean = roles.contains(Token.ADMIN)
  def isUser: Boolean = roles.contains(Token.USER)
}

object Token {
  final val ADMIN = 'ADMIN
  final val USER = 'USER
  def apply(value: String): Token = new Token(value, JNothing, sub = "", name = "", email = "", roles = Nil)
}

trait BearerAuthenticationSupport extends ScentrySupport[Token] with BearerAuthSupport[Token] {

  self: ScalatraBase =>

  override protected def fromSession: PartialFunction[String, Token] = {
    case a => Token(a)
  }

  override protected def toSession: PartialFunction[Token, String] = {
    case a => a.value
  }

  override protected val scentryConfig: ScentryConfiguration = {
    new ScentryConfig {}.asInstanceOf[ScentryConfiguration]
  }

  override def realm: String = "Ubirch Token Service"

}

