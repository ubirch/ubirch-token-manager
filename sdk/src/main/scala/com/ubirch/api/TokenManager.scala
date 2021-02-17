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

object TokenVerification {
  final val ISSUER = "iss"
  final val SUBJECT = "sub"
  final val AUDIENCE = "aud"
  final val EXPIRATION = "exp"
  final val NOT_BEFORE = "nbf"
  final val ISSUED_AT = "iat"
  final val JWT_ID = "jti"
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

  val issuer: String = {
    (for {
      JObject(child) <- all
      JField(TokenVerification.ISSUER, JString(value)) <- child
    } yield {
      value
    }).headOption.getOrElse("")
  }

  val subject: String = {
    (for {
      JObject(child) <- all
      JField(TokenVerification.SUBJECT, JString(value)) <- child
    } yield {
      value
    }).headOption.getOrElse("")
  }

  val audiences: List[String] = {
    for {
      JObject(child) <- all
      JField(TokenVerification.AUDIENCE, JArray(values)) <- child
      JString(value) <- values
    } yield {
      value
    }
  }

  val purpose: String = {
    (for {
      JObject(child) <- all
      JField("purpose", JString(value)) <- child
    } yield {
      value
    }).headOption
      .getOrElse("")
  }

  val targetIdentities: List[UUID] = {
    for {
      JObject(child) <- all
      JField("target_identities", JArray(ids)) <- child
      JString(id) <- ids
      tryUUID = Try(UUID.fromString(id))
      if tryUUID.isSuccess
    } yield {
      tryUUID.get
    }
  }

  val isWildCard: Boolean = {
    (for {
      JObject(child) <- all
      JField("target_identities", JArray(ids)) <- child
      JString(id) <- ids
      if id == "*"
    } yield {
      id
    }).nonEmpty
  }

  val scopes: List[String] = {
    for {
      JObject(child) <- all
      JField("scopes", JArray(scopes)) <- child
      JString(scope) <- scopes
    } yield {
      scope
    }
  }

  val originDomains: List[URL] = {
    for {
      JObject(child) <- all
      JField("origin_domains", JArray(urls)) <- child
      JString(url) <- urls
      tryURL = Try(new URL(url))
      if tryURL.isSuccess
    } yield {
      tryURL.get
    }
  }

  def validatePurpose: Try[String] = {
    Try(purpose.nonEmpty && purpose.length > 3).map(_ => purpose)
  }

  def validateUUID(uuid: UUID): Try[UUID] = {
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

}
