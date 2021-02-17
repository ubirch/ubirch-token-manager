package com.ubirch.api

import java.net.URL
import java.security.PublicKey
import java.util.UUID

import com.ubirch.crypto.PubKey
import com.ubirch.defaults.{ InvalidOrigin, InvalidSpecificClaim, InvalidUUID }
import org.json4s.{ JValue, JsonInput }

import scala.util.{ Failure, Success, Try }

trait TokenManager {

  def decodeAndVerify(jwt: String): Try[Claims]
  def getClaims(token: String): Try[Claims]

}

trait TokenPublicKey {
  def pubKey: PubKey
  def publicKey: PublicKey = pubKey.getPublicKey
}

trait TokenVerification {
  def decodeAndVerify(jwt: String): Try[Claims]
}

object TokenVerification {
  final val ISSUER = "iss"
  final val SUBJECT = "sub"
  final val AUDIENCE = "aud"
  final val EXPIRATION = "exp"
  final val NOT_BEFORE = "nbf"
  final val ISSUED_AT = "iat"
  final val JWT_ID = "jti"

  implicit class EnrichedAll(all: Map[String, Any]) {
    def getSubject: Try[UUID] = all.get(SUBJECT).toRight(InvalidSpecificClaim("Invalid subject", all.toString()))
      .toTry
      .map(_.asInstanceOf[String])
      .filter(_.nonEmpty)
      .map(UUID.fromString)
      .recover { case e: Exception => throw InvalidSpecificClaim("Invalid subject", e.getMessage) }

  }

}

trait JsonConverterService {
  def toString(value: JValue): String
  def toString[T](t: T): Either[Exception, String]
  def toJValue(value: String): Either[Exception, JValue]
  def toJValue[T](obj: T): Either[Exception, JValue]
  def as[T: Manifest](value: String): Either[Exception, T]
  def fromJsonInput[T](json: JsonInput)(f: JValue => JValue)(implicit mf: Manifest[T]): T
}

case class Content(
    purpose: String,
    targetIdentities: Either[List[UUID], String],
    originDomains: List[URL],
    scopes: List[String]
)

case class Claims(token: String, all: Map[String, Any], content: Content) {

  def validatePurpose: Try[String] = {
    Try(content.purpose.nonEmpty && content.purpose.length > 3).map(_ => content.purpose)
  }

  def validateUUID(uuid: UUID): Try[UUID] = {
    val res = content.targetIdentities match {
      case Left(uuids) => uuids.contains(uuid)
      case Right(wildcard) => wildcard == "*"
    }
    if (res) Success(uuid)
    else Failure(InvalidUUID("Invalid UUID", s"upp_uuid_not_equals_target_identities=${uuid} != ${content.targetIdentities.left.map(_.map(_.toString))}"))
  }

  def validateOrigin(maybeOrigin: Option[String]): Try[List[URL]] = {
    (for {
      origin <- Try(maybeOrigin.filter(_.nonEmpty).map(x => new URL(x)))
      res <- Try(content.originDomains.forall(x => Option(x) == origin))
    } yield res) match {
      case Success(true) => Success(content.originDomains)
      case _ =>
        Failure(InvalidOrigin("Invalid Origin", s"origin_not_equals_origin_domains=${maybeOrigin.filter(_.nonEmpty).getOrElse("NO-ORIGIN")} != ${content.originDomains.map(_.toString).mkString(",")}"))
    }
  }

}
