package com.ubirch.models

import java.net.URL
import java.util.UUID

import org.json4s.JsonAST.JString
import org.json4s.JsonDSL._
import org.json4s.{ CustomSerializer, Formats, JObject, JsonAST, MappingException }

import scala.util.Try
import scala.util.control.NonFatal

class TokenPurposedClaimSerializer(implicit formats: Formats) extends CustomSerializer[TokenPurposedClaim](_ =>
  ({
    case jsonObj: JObject =>

      try {

        val tenantId: UUID = (jsonObj \ "tenantId").extract[UUID]
        val purpose: String = (jsonObj \ "purpose").extract[String]
        val targetIdentities: Either[List[UUID], List[String]] = {
          jsonObj \ "targetIdentities" match {
            case JsonAST.JArray(arr) =>
              val tgs = for { JString(s) <- arr } yield s
              Try { tgs.map(UUID.fromString) }.toEither.left.map(_ => tgs).swap
            case JsonAST.JString(s) => Right(List(s))
            case _ => Right(Nil)
          }
        }
        val targetGroups: Either[List[UUID], List[String]] = {
          jsonObj \ "targetGroups" match {
            case JsonAST.JArray(arr) =>
              val tgs = for { JString(s) <- arr } yield s
              Try { tgs.map(UUID.fromString) }.toEither.left.map(_ => tgs).swap
            case JsonAST.JString(s) => Right(List(s))
            case _ => Right(Nil)
          }
        }
        val expiration: Option[Long] = (jsonObj \ "expiration").extractOpt[Long]
        val notBefore: Option[Long] = (jsonObj \ "notBefore").extractOpt[Long]
        val originDomains: List[URL] = (jsonObj \ "originDomains").extractOpt[List[URL]].getOrElse(Nil)
        val scopes: List[String] = (jsonObj \ "scopes").extractOpt[List[String]].getOrElse(Nil)

        TokenPurposedClaim(
          tenantId = tenantId,
          purpose = purpose,
          targetIdentities = targetIdentities,
          targetGroups = targetGroups,
          expiration = expiration,
          notBefore = notBefore,
          originDomains = originDomains,
          scopes = scopes
        )

      } catch {
        case NonFatal(e) =>
          throw MappingException(e.getMessage, new java.lang.IllegalArgumentException(e))
      }

  }, {
    case tokenClaim: TokenPurposedClaim =>
      ("tenantId" -> tokenClaim.tenantId.toString) ~
        ("purpose" -> tokenClaim.purpose) ~
        ("targetIdentities" -> tokenClaim.targetIdentities.left.map(_.map(_.toString)).merge) ~
        ("targetGroups" -> tokenClaim.targetGroups.left.map(_.map(_.toString)).merge) ~
        ("expiration" -> tokenClaim.expiration) ~
        ("notBefore" -> tokenClaim.notBefore) ~
        ("originDomains" -> tokenClaim.originDomains.map(_.toString)) ~
        ("scopes" -> tokenClaim.scopes)
  }))
