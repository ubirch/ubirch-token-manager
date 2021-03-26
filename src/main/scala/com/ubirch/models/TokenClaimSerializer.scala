package com.ubirch.models

import java.util.UUID

import org.json4s.JsonDSL._
import org.json4s.{ CustomSerializer, Formats, JObject, MappingException }

import scala.util.control.NonFatal

class TokenClaimSerializer(implicit formats: Formats) extends CustomSerializer[TokenClaim](_ =>
  ({
    case jsonObj: JObject =>

      try {

        val ownerId: UUID = (jsonObj \ "ownerId").extract[UUID]
        val issuer: String = (jsonObj \ "issuer").extract[String]
        val subject: String = (jsonObj \ "subject").extract[String]
        val audience: List[String] = {
          val key = jsonObj \ "audience"
          key.extractOrElse[List[String]](key.extractOpt[String].toList)
        }
        val expiration: Option[Long] = (jsonObj \ "expiration").extractOpt[Long]
        val notBefore: Option[Long] = (jsonObj \ "notBefore").extractOpt[Long]
        val issuedAt: Option[Long] = (jsonObj \ "issuedAt").extractOpt[Long]
        val content: Map[Symbol, Any] = (jsonObj \ "content").extract[Map[Symbol, Any]]

        TokenClaim(
          ownerId = ownerId,
          issuer = issuer,
          subject = subject,
          audience = audience,
          expiration = expiration,
          notBefore = notBefore,
          issuedAt = issuedAt,
          content = content
        )

      } catch {
        case NonFatal(e) =>
          throw MappingException(e.getMessage, new java.lang.IllegalArgumentException(e))
      }

  }, {
    case tokenClaim: TokenClaim =>
      ("ownerId" -> tokenClaim.ownerId.toString) ~
        ("issuer" -> tokenClaim.issuer) ~
        ("subject" -> tokenClaim.subject) ~
        ("audience" -> tokenClaim.audience) ~
        ("expiration" -> tokenClaim.expiration) ~
        ("notBefore" -> tokenClaim.notBefore) ~
        ("issuedAt" -> tokenClaim.issuedAt) ~
        ("content" -> tokenClaim.content.map { case (k, v) => k.name -> v.toString })
  }))
