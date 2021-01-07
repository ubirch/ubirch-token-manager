package com.ubirch.services.jwt

import java.util.{ Date, UUID }

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.ConfPaths.GenericConfPaths
import com.ubirch.controllers.concerns.Token
import com.ubirch.models.{ TokenClaim, TokenCreationData, TokenRow, TokenVerificationClaim, TokensDAO }
import com.ubirch.util.TaskHelpers
import com.ubirch.{ InvalidSpecificClaim, TokenEncodingException }
import javax.inject.{ Inject, Singleton }
import monix.eval.Task

trait TokenStoreService {
  def create(accessToken: Token, tokenClaim: TokenClaim, category: Symbol): Task[TokenCreationData]
  def create(accessToken: Token, tokenClaim: TokenVerificationClaim): Task[TokenCreationData]
  def list(accessToken: Token): Task[List[TokenRow]]
  def get(accessToken: Token, id: UUID): Task[Option[TokenRow]]
  def delete(accessToken: Token, tokenId: UUID): Task[Boolean]
}

@Singleton
class DefaultTokenStoreService @Inject() (config: Config, tokenKey: TokenKeyService, tokenCreation: TokenCreationService, tokensDAO: TokensDAO) extends TokenStoreService with TaskHelpers with LazyLogging {

  private final val ENV = config.getString(GenericConfPaths.ENV)

  override def create(accessToken: Token, tokenClaim: TokenClaim, category: Symbol): Task[TokenCreationData] = {

    for {
      _ <- earlyResponseIf(UUID.fromString(accessToken.id) != tokenClaim.ownerId)(InvalidSpecificClaim(s"Owner Id is invalid (${accessToken.id} ${tokenClaim.ownerId})", accessToken.id))

      jwtID = UUID.randomUUID()

      res <- liftTry(tokenCreation.encode(jwtID, tokenClaim, tokenKey.key))(TokenEncodingException("Error creating token", tokenClaim))
      (token, claims) = res

      _ <- earlyResponseIf(claims.jwtId.isEmpty)(TokenEncodingException("No token id found", tokenClaim))
      aRow = TokenRow(UUID.fromString(claims.jwtId.get), tokenClaim.ownerId, token, category.name, new Date())

      insertion <- tokensDAO.insert(aRow).headOptionL

      _ = if (insertion.isEmpty) logger.error("failed_token_insertion={}", tokenClaim.toString)
      _ = if (insertion.isDefined) logger.info("token_insertion_succeeded={}", tokenClaim.toString)

    } yield {
      TokenCreationData(jwtID, claims, token)
    }

  }

  override def create(accessToken: Token, tokenVerificationClaim: TokenVerificationClaim): Task[TokenCreationData] = {
    for {
      _ <- Task.unit

      _ <- earlyResponseIf(!tokenVerificationClaim.validatePurpose)(InvalidSpecificClaim("Invalid Purpose", "Purpose is not correct."))
      _ <- earlyResponseIf(!tokenVerificationClaim.validateIdentities)(InvalidSpecificClaim("Invalid Target Identities", "Target Identities are empty or invalid"))

      targetIdentities = tokenVerificationClaim.targetIdentities match {
        case Left(uuids) => 'target_identities -> uuids.distinct.map(_.toString).asInstanceOf[Any]
        case Right(other) => 'target_identities -> other.asInstanceOf[Any]
      }

      tokenClaim = TokenClaim(
        ownerId = tokenVerificationClaim.tenantId,
        issuer = s"https://token.$ENV.ubirch.com",
        subject = tokenVerificationClaim.tenantId.toString,
        audience = s"https://verify.$ENV.ubirch.com",
        expiration = tokenVerificationClaim.expiration,
        notBefore = tokenVerificationClaim.notBefore,
        issuedAt = None,
        content = Map(
          'purpose -> tokenVerificationClaim.purpose,
          targetIdentities,
          'role -> "verifier",
          'scope -> "ver"
        )
      )

      tokeCreationData <- create(accessToken, tokenClaim, 'verification)

    } yield {
      tokeCreationData
    }
  }

  override def list(accessToken: Token): Task[List[TokenRow]] = {
    for {
      ownerId <- Task(UUID.fromString(accessToken.id))
      rows <- tokensDAO.byOwnerId(ownerId).toListL
    } yield {
      rows
    }
  }

  override def get(accessToken: Token, id: UUID): Task[Option[TokenRow]] = {
    for {
      ownerId <- Task(UUID.fromString(accessToken.id))
      rows <- tokensDAO.byOwnerIdAndId(ownerId, id).headOptionL
    } yield {
      rows
    }
  }

  override def delete(accessToken: Token, tokenId: UUID): Task[Boolean] = {
    for {
      ownerId <- Task(UUID.fromString(accessToken.id))
      deletion <- tokensDAO.delete(ownerId, tokenId).headOptionL

      _ = if (deletion.isEmpty) logger.error("failed_token_deletion={}", tokenId.toString)
      _ = if (deletion.isDefined) logger.info("token_deletion_succeeded={}", tokenId.toString)
    } yield {
      deletion.isDefined
    }
  }

}
