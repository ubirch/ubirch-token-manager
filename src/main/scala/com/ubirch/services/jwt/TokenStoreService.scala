package com.ubirch.services.jwt

import java.util.{ Date, UUID }

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.ConfPaths.{ GenericConfPaths, TokenGenPaths }
import com.ubirch.controllers.concerns.Token
import com.ubirch.crypto.GeneratorKeyFactory
import com.ubirch.crypto.utils.Curve
import com.ubirch.models.{ TokenClaim, TokenCreationData, TokenRow, TokenVerificationClaim, TokensDAO }
import com.ubirch.util.TaskHelpers
import com.ubirch.{ InvalidSpecificClaim, TokenEncodingException }
import javax.inject.{ Inject, Singleton }
import monix.eval.Task

trait TokenStoreService {
  def create(accessToken: Token, tokenClaim: TokenClaim): Task[TokenCreationData]
  def create(accessToken: Token, tokenClaim: TokenVerificationClaim): Task[TokenCreationData]
  def list(accessToken: Token): Task[List[TokenRow]]
  def delete(accessToken: Token, tokenId: UUID): Task[Boolean]
}

@Singleton
class DefaultTokenStoreService @Inject() (config: Config, tokenCreation: TokenCreationService, tokensDAO: TokensDAO) extends TokenStoreService with TaskHelpers with LazyLogging {

  private final val privKey = GeneratorKeyFactory.getPrivKey(config.getString(TokenGenPaths.PRIV_KEY_IN_HEX), Curve.PRIME256V1)
  private final val ENV = config.getString(GenericConfPaths.ENV)

  override def create(accessToken: Token, tokenClaim: TokenClaim): Task[TokenCreationData] = {

    for {
      _ <- earlyResponseIf(UUID.fromString(accessToken.id) != tokenClaim.ownerId)(InvalidSpecificClaim(s"Owner Id is invalid (${accessToken.id} ${tokenClaim.ownerId})", accessToken.id))

      jwtID = UUID.randomUUID()

      res <- liftTry(tokenCreation.encode(jwtID, tokenClaim, privKey))(TokenEncodingException("Error creating token", tokenClaim))
      (token, claims) = res

      _ = earlyResponseIf(claims.jwtId.isEmpty)(TokenEncodingException("No token id found", tokenClaim))
      aRow = TokenRow(UUID.fromString(claims.jwtId.get), tokenClaim.ownerId, token, new Date())

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
          'target_identity -> tokenVerificationClaim.target_identity.toString
        )
      )

      tokeCreationData <- create(accessToken, tokenClaim)

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
