package com.ubirch.services.jwt

import java.util.{ Date, UUID }

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.{ InvalidSpecificClaim, TokenEncodingException }
import com.ubirch.controllers.concerns.Token
import com.ubirch.crypto.GeneratorKeyFactory
import com.ubirch.crypto.utils.Curve
import com.ubirch.models.{ TokenClaim, TokenCreationData, TokenRow, TokensDAO }
import com.ubirch.util.TaskHelpers
import javax.inject.{ Inject, Singleton }
import monix.eval.Task

trait TokenStoreService {
  def create(accessToken: Token, tokenClaim: TokenClaim): Task[TokenCreationData]
}

@Singleton
class DefaultTokenStoreService @Inject() (config: Config, tokenCreation: TokenCreationService, tokensDAO: TokensDAO) extends TokenStoreService with TaskHelpers with LazyLogging {

  private final val privKey = GeneratorKeyFactory.getPrivKey(config.getString("tokenSystem.tokenGen.privKeyInHex"), Curve.PRIME256V1)

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

}
