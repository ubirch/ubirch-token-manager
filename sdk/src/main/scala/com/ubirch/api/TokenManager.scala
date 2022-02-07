package com.ubirch.api

import java.net.URL
import java.security.PublicKey
import java.util.{ Date, UUID }

import com.ubirch.crypto.PubKey
import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.schedulers.CanBlock
import org.json4s.JsonAST.{ JArray, JField }
import org.json4s.{ JObject, JString, JValue, JsonInput }

import javax.crypto.SecretKey

import scala.concurrent.duration.Duration
import scala.util.{ Failure, Success, Try }

trait TokenManager {
  def decodeAndVerify(jwt: String): Try[Claims]
  def getClaims(token: String): Try[Claims]
  def externalStateVerify(accessToken: String, identity: UUID): Task[Boolean]
  def externalStateVerifySync(accessToken: String, identity: UUID)(timeout: Duration)(implicit s: Scheduler, permit: CanBlock): Either[Throwable, Boolean]
}

trait TokenPublicKey {
  def pubKey: PubKey
  def publicKey: PublicKey = pubKey.getPublicKey
}

trait TokenVerification {
  def decodeAndVerify(jwt: String): Try[Claims]
}

trait HMAC {
  def getHMAC(data: Array[Byte], macKey: SecretKey): String
  def getHMAC(data: Array[Byte], time: Date): String
}

trait JsonConverterService {
  def toString(value: JValue): String
  def toString[T](t: T): Either[Exception, String]
  def toJValue(value: String): Either[Exception, JValue]
  def toJValue[T](obj: T): Either[Exception, JValue]
  def as[T: Manifest](value: String): Either[Exception, T]
  def fromJsonInput[T](json: JsonInput)(f: JValue => JValue)(implicit mf: Manifest[T]): T
}

case class VerificationRequest(token: String, identity: UUID)

trait ExternalStateGetter {
  def verify(body: Array[Byte]): ExternalResponseData[Array[Byte]]
}

trait ExternalStateVerifier {
  def verify(verificationRequest: VerificationRequest): Task[Boolean]
}

case class ExternalResponseData[T](status: Int, headers: Map[String, List[String]], body: T)

class TokenSDKException(message: String, value: String) extends Exception(message) {
  def getValue: String = value
}

case class InvalidClaimException(message: String, value: String) extends TokenSDKException(message, value)

class Claims(val token: String, val all: JValue) {

  import Claims._

  val issuer: String = extractString(ISSUER, all)
  val subject: String = extractString(SUBJECT, all)
  val audiences: List[String] = extractListString(AUDIENCE, all) match {
    case Nil => List(extractString(AUDIENCE, all))
    case xs => xs
  }
  val purpose: String = extractString(PURPOSE_KEY.name, all)
  val targetIdentities: Either[List[UUID], List[String]] = extractListUUID(TARGET_IDENTITIES_KEY.name, all) match {
    case Nil => Right(extractListString(TARGET_IDENTITIES_KEY.name, all).distinct)
    case xs => Left(xs.distinct)
  }
  val targetIdentitiesMerged: List[String] = targetIdentities.left.map(_.map(_.toString)).merge
  val isTargetIdentitiesStarWildCard: Boolean = targetIdentities.right.map(_.contains("*")).fold(_ => false, x => x)
  val hasMaybeTargetIdentities: Boolean = targetIdentitiesMerged.exists(_.nonEmpty)

  val targetGroups: Either[List[UUID], List[String]] = extractListUUID(TARGET_GROUPS_KEY.name, all) match {
    case Nil => Right(extractListString(TARGET_GROUPS_KEY.name, all).distinct)
    case xs => Left(xs.distinct)
  }
  val hasMaybeGroups: Boolean = targetGroups.left.map(_.map(_.toString)).merge.exists(_.nonEmpty)

  val scopes: List[String] = extractListString(SCOPES_KEY.name, all)
  val originDomains: List[URL] = extractListURL(ORIGIN_KEY.name, all)

  def findScope(scope: String): Option[String] = scopes.find(_ == scope)

  def hasScope(scope: String): Boolean = findScope(scope).isDefined

  def hasScopes: Boolean = scopes.nonEmpty

  def validateScope(scope: String): Try[String] = {
    findScope(scope)
      .toRight(InvalidClaimException("Invalid Scope", s"scope_not_found_in_expected_scopes=$scope"))
      .toTry
  }

  def validatePurpose: Try[String] = Try(purpose.nonEmpty && purpose.length > 3).map(_ => purpose)

  def validateIdentity(uuid: UUID): Try[UUID] = {
    val res =
      if (isTargetIdentitiesStarWildCard) true
      else targetIdentities.left.map(_.contains(uuid)) match {
        case Left(validation) => validation
        case Right(_) => false
      }

    if (res) Success(uuid)
    else Failure(InvalidClaimException("Invalid UUID", s"upp_uuid_not_equals_target_identities=$uuid != ${targetIdentitiesMerged.mkString(",")}"))
  }

  def validateOrigin(maybeOrigin: Option[String]): Try[List[URL]] = {
    (for {
      origin <- Try(maybeOrigin.filter(_.nonEmpty).map(x => new URL(x)))
      res <- Try(originDomains.forall(x => Option(x) == origin))
    } yield res) match {
      case Success(true) => Success(originDomains)
      case _ =>
        Failure(InvalidClaimException("Invalid Origin", s"origin_not_equals_origin_domains=${maybeOrigin.filter(_.nonEmpty).getOrElse("NO-ORIGIN")} != ${originDomains.map(_.toString).mkString(",")}"))
    }
  }

  def validateSubject(subject: String): Try[String] = {
    if (subject == this.subject) Success(subject)
    else Failure(InvalidClaimException("Invalid Subject", s"subject_not_equal_to=$subject"))
  }

  def validateSubjectAsUUID(subject: UUID): Try[UUID] = {
    isSubjectUUID.flatMap { uuid =>
      if (subject == uuid) Success(uuid)
      else Failure(InvalidClaimException("Invalid Subject", s"subject_not_equal_to=$subject"))
    }
  }

  def isSubjectUUID: Try[UUID] = Try(UUID.fromString(subject)).recoverWith {
    case _: Exception =>
      Failure(InvalidClaimException("Invalid Subject As UUID", s"subject_not_convertible_to_uuid=$subject"))
  }

}

object Claims {

  final val ISSUER = "iss"
  final val SUBJECT = "sub"
  final val AUDIENCE = "aud"
  final val EXPIRATION = "exp"
  final val NOT_BEFORE = "nbf"
  final val ISSUED_AT = "iat"
  final val JWT_ID = "jti"

  final val PURPOSE_KEY = 'pur
  final val TARGET_IDENTITIES_KEY = 'tid
  final val TARGET_GROUPS_KEY = 'tgp
  final val ORIGIN_KEY = 'ord
  final val SCOPES_KEY = 'scp

  def extractStringAsOpt(key: String, obj: JValue): Option[String] = {
    (for {
      JObject(child) <- obj
      JField(k, JString(value)) <- child if k == key
    } yield value).headOption
  }

  def extractString(key: String, obj: JValue): String = extractStringAsOpt(key, obj).getOrElse("")

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
