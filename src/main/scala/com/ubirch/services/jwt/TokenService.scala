package com.ubirch.services.jwt

import java.util.{ Date, UUID }

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.ConfPaths.GenericConfPaths
import com.ubirch.controllers.concerns.Token
import com.ubirch.models.Scope.asString
import com.ubirch.models._
import com.ubirch.util.TaskHelpers
import com.ubirch.{ InvalidClaimException, TokenEncodingException }
import monix.eval.Task

import javax.inject.{ Inject, Singleton }

import com.ubirch.services.formats.JsonConverterService
import com.ubirch.services.key.HMACVerifier
import com.ubirch.services.state.StateVerifier

trait TokenService {
  def create(accessToken: Token, tokenClaim: TokenClaim, category: Symbol): Task[TokenCreationData]
  def create(accessToken: Token, tokenClaim: TokenPurposedClaim): Task[TokenCreationData]
  def list(accessToken: Token): Task[List[TokenRow]]
  def get(accessToken: Token, id: UUID): Task[Option[TokenRow]]
  def delete(accessToken: Token, tokenId: UUID): Task[Boolean]
  def verify(verificationRequest: VerificationRequest): Task[Boolean]
  def processBootstrapToken(bootstrapToken: String): Task[BootstrapToken]
}

@Singleton
class DefaultTokenService @Inject() (
    config: Config,
    tokenKey: TokenKeyService,
    tokenEncodingService: TokenEncodingService,
    tokenDecodingService: TokenDecodingService,
    stateVerifier: StateVerifier,
    tokensDAO: TokensDAO,
    HMACVerifier: HMACVerifier,
    jsonConverterService: JsonConverterService
) extends TokenService with TaskHelpers with LazyLogging {

  private final val ENV = config.getString(GenericConfPaths.ENV)

  override def create(accessToken: Token, tokenClaim: TokenClaim, category: Symbol): Task[TokenCreationData] = {
    for {
      _ <- earlyResponseIf(UUID.fromString(accessToken.id) != tokenClaim.ownerId)(InvalidClaimException(s"Owner Id is invalid (${accessToken.id} ${tokenClaim.ownerId})", accessToken.id))
      tcd <- create(tokenClaim, category)
    } yield {
      tcd
    }
  }

  override def create(accessToken: Token, tokenPurposedClaim: TokenPurposedClaim): Task[TokenCreationData] = {
    for {
      _ <- localVerify(tokenPurposedClaim)
      groupsCheck <- verifyGroupsForCreation(tokenPurposedClaim)
      _ <- earlyResponseIf(!groupsCheck)(InvalidClaimException("Invalid Groups", "Groups couldn't be validated"))

      tokenClaim = tokenPurposedClaim.toTokenClaim(ENV)
      tokeCreationData <- create(accessToken, tokenClaim, 'purposed_claim)

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

  override def verify(verificationRequest: VerificationRequest): Task[Boolean] = {
    for {
      sigOK <- Task.delay(HMACVerifier.verify(verificationRequest))
      _ <- earlyResponseIf(!sigOK)(InvalidClaimException("Invalid Request Signature", "The hmac verification failed"))

      tokenPurposedClaim <- buildTokenClaimFromVerificationRequest(verificationRequest)
      _ <- localVerify(tokenPurposedClaim)
      groupsCheck <- verifyGroupsForVerificationRequest(verificationRequest, tokenPurposedClaim)

      _ <- earlyResponseIf(!groupsCheck)(InvalidClaimException("Invalid Groups", "Groups couldn't be validated"))

    } yield {
      true
    }
  }

  override def processBootstrapToken(bootstrapToken: String): Task[BootstrapToken] = {

    def go(scope: Scope): Task[TokenCreationData] = {
      for {
        tokenPurposedClaim <- buildTokenClaimFromUbirchTokenAsString(bootstrapToken).map(_.copy())
        tokenClaim = tokenPurposedClaim
          .copy(scopes = List(asString(scope)))
          .toTokenClaim(ENV)

        tokeCreationData <- create(tokenClaim, 'purposed_claim)
      } yield tokeCreationData
    }

    for {
      thingCreate <- go(Scope.Thing_Create)
      thingAnchor <- go(Scope.UPP_Anchor)
      thingVerify <- go(Scope.UPP_Verify)
    } yield {
      BootstrapToken(thingCreate, thingAnchor, thingVerify)
    }

  }

  ///// private stuff
  private def create(tokenClaim: TokenClaim, category: Symbol): Task[TokenCreationData] = {
    for {
      _ <- Task.unit // here to make the compiler happy

      jwtID = UUID.randomUUID()

      res <- liftTry(tokenEncodingService.encode(jwtID, tokenClaim, tokenKey.key))(TokenEncodingException("Error creating token", tokenClaim))
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

  private def buildTokenClaimFromVerificationRequest(verificationRequest: VerificationRequest): Task[TokenPurposedClaim] = {
    buildTokenClaimFromUbirchTokenAsString(verificationRequest.token)
  }

  private def buildTokenClaimFromUbirchTokenAsString(token: String): Task[TokenPurposedClaim] = {
    for {
      tokenJValue <- Task.fromTry(tokenDecodingService.decodeAndVerify(token, tokenKey.key.getPublicKey))
      tokenString <- Task.delay(jsonConverterService.toString(tokenJValue))
      tokenPurposedClaim <- Task.delay(jsonConverterService.fromJsonInput[TokenPurposedClaim](tokenString) { x =>
        //We can improve this matcher later
        x.camelizeKeys.transformField {
          case ("sub", value) => ("tenantId", value)
          case ("pur", value) => ("purpose", value)
          case ("tid", value) => ("targetIdentities", value)
          case ("tgp", value) => ("targetGroups", value)
          case ("exp", value) => ("expiration", value)
          case ("ord", value) => ("originDomains", value)
          case ("scp", value) => ("scopes", value)
        }
      })
    } yield {
      tokenPurposedClaim
    }
  }

  private def localVerify(tokenPurposedClaim: TokenPurposedClaim): Task[Boolean] = for {
    _ <- earlyResponseIf(tokenPurposedClaim.hasMaybeGroups && tokenPurposedClaim.hasMaybeIdentities)(InvalidClaimException("Invalid Target Identities or Groups", "Either have identities or groups"))
    _ <- earlyResponseIf(!tokenPurposedClaim.validatePurpose)(InvalidClaimException("Invalid Purpose", "Purpose is not correct."))
    _ <- earlyResponseIf(!tokenPurposedClaim.hasMaybeGroups && !tokenPurposedClaim.validateIdentities)(InvalidClaimException("Invalid Target Identities", "Target Identities are empty or invalid"))
    _ <- earlyResponseIf(!tokenPurposedClaim.validateOriginsDomains)(InvalidClaimException("Invalid Origin Domains", "Origin Domains are empty or invalid"))
    _ <- earlyResponseIf(!tokenPurposedClaim.validateScopes)(InvalidClaimException(s"Invalid Scopes :: ${tokenPurposedClaim.scopes}", "Scopes are empty or invalid"))
  } yield true

  private def verifyGroupsForCreation(tokenPurposedClaim: TokenPurposedClaim): Task[Boolean] = {
    if (tokenPurposedClaim.hasMaybeGroups) {
      stateVerifier
        .tenantGroups(tokenPurposedClaim.tenantId)
        .map { gs => verifyGroups(tokenPurposedClaim, gs) }
    } else Task.delay(true)
  }

  private def verifyGroupsForVerificationRequest(verificationRequest: VerificationRequest, tokenPurposedClaim: TokenPurposedClaim): Task[Boolean] = {
    if (tokenPurposedClaim.hasMaybeGroups) {
      stateVerifier
        .identityGroups(verificationRequest.identity)
        .map { gs => verifyGroups(tokenPurposedClaim, gs) }
    } else Task.delay(true)
  }

  private def verifyGroups(tokenPurposedClaim: TokenPurposedClaim, currentGroups: List[Group]): Boolean = {
    tokenPurposedClaim.targetGroups match {
      case Right(names) if currentGroups.nonEmpty => currentGroups.map(_.name).intersect(names).sorted == names.sorted
      case Left(uuids) if currentGroups.nonEmpty => currentGroups.map(_.id).intersect(uuids).sorted == uuids.sorted
      case _ => false
    }
  }

}
