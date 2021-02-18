package com.ubirch.api

import java.net.URL
import java.security.PublicKey
import java.util.UUID

import com.ubirch.crypto.PubKey
import com.ubirch.defaults.{ InvalidOrigin, InvalidUUID }
import org.json4s.JsonAST.{ JArray, JField }
import org.json4s.{ JObject, JString, JValue, JsonInput }

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

trait JsonConverterService {
  def toString(value: JValue): String
  def toString[T](t: T): Either[Exception, String]
  def toJValue(value: String): Either[Exception, JValue]
  def toJValue[T](obj: T): Either[Exception, JValue]
  def as[T: Manifest](value: String): Either[Exception, T]
  def fromJsonInput[T](json: JsonInput)(f: JValue => JValue)(implicit mf: Manifest[T]): T
}

class Claims(val token: String, val all: JValue) {

  import Claims._

  val issuer: String = extractString(ISSUER, all)
  val subject: String = extractString(SUBJECT, all)
  val audiences: List[String] = extractListString(AUDIENCE, all) match {
    case Nil => List(extractString(AUDIENCE, all))
    case xs => xs
  }

  val purpose: String = extractString("purpose", all)
  val targetIdentities: List[UUID] = extractListUUID("target_identities", all)
  val isWildCard: Boolean = extractListString("target_identities", all).contains("*")
  val scopes: List[String] = extractListString("scopes", all)
  val originDomains: List[URL] = extractListURL("origin_domains", all)

  def validatePurpose: Try[String] = Try(purpose.nonEmpty && purpose.length > 3).map(_ => purpose)

  def validateIdentity(uuid: UUID): Try[UUID] = {
    val res = if (isWildCard) true else targetIdentities.contains(uuid)
    if (res) Success(uuid)
    else Failure(InvalidUUID("Invalid UUID", s"upp_uuid_not_equals_target_identities=${uuid} != ${targetIdentities.map(_.toString).mkString(",")}"))
  }

  def validateOrigin(maybeOrigin: Option[String]): Try[List[URL]] = {
    (for {
      origin <- Try(maybeOrigin.filter(_.nonEmpty).map(x => new URL(x)))
      res <- Try(originDomains.forall(x => Option(x) == origin))
    } yield res) match {
      case Success(true) => Success(originDomains)
      case _ =>
        Failure(InvalidOrigin("Invalid Origin", s"origin_not_equals_origin_domains=${maybeOrigin.filter(_.nonEmpty).getOrElse("NO-ORIGIN")} != ${originDomains.map(_.toString).mkString(",")}"))
    }
  }

  def isSubjectUUID: Try[UUID] = Try(UUID.fromString(subject))

}

object Claims {

  final val ISSUER = "iss"
  final val SUBJECT = "sub"
  final val AUDIENCE = "aud"
  final val EXPIRATION = "exp"
  final val NOT_BEFORE = "nbf"
  final val ISSUED_AT = "iat"
  final val JWT_ID = "jti"

  def extractString(key: String, obj: JValue): String = {
    (for {
      JObject(child) <- obj
      JField(k, JString(value)) <- child if k == key
    } yield value).headOption.getOrElse("")
  }

  def extractListString(key: String, obj: JValue): List[String] = {
    for {
      JObject(child) <- obj
      JField(k, JArray(scopes)) <- child if k == key
      JString(scope) <- scopes
    } yield scope
  }

  def extractListUUID(key: String, obj: JValue): List[UUID] = {
    for {
      JObject(child) <- obj
      JField(k, JArray(ids)) <- child if k == key
      JString(id) <- ids
      tryUUID = Try(UUID.fromString(id)) if tryUUID.isSuccess
    } yield tryUUID.get
  }

  def extractListURL(key: String, obj: JValue): List[URL] = {
    for {
      JObject(child) <- obj
      JField(k, JArray(urls)) <- child if k == key
      JString(url) <- urls
      tryURL = Try(new URL(url)) if tryURL.isSuccess
    } yield tryURL.get
  }

}
