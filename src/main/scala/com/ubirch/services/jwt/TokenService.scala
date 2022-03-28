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
import pdi.jwt.exceptions.JwtValidationException

trait TokenService {
  def create(accessToken: Token, tokenClaim: TokenClaim, category: Symbol): Task[TokenCreationData]
  def create(accessToken: Token, tokenClaim: TokenPurposedClaim): Task[TokenCreationData]
  def list(accessToken: Token): Task[List[TokenRow]]
  def get(accessToken: Token, id: UUID): Task[Option[TokenRow]]
  def delete(accessToken: Token, tokenId: UUID): Task[Boolean]
  def verify(verificationRequest: VerificationRequest): Task[Boolean]
  def processBootstrapToken(bootstrapRequest: BootstrapRequest): Task[BootstrapToken]
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
      _ <- earlyResponseIf(UUID.fromString(accessToken.id) != tokenClaim.ownerId)(InvalidClaimException(s"Owner Id is invalid (${accessToken.id} != ${tokenClaim.ownerId})", s"${accessToken.id} != ${tokenClaim.ownerId}"))
      tcd <- create(tokenClaim, category)
    } yield {
      tcd
    }
  }

  override def create(accessToken: Token, tokenPurposedClaim: TokenPurposedClaim): Task[TokenCreationData] = {
    for {
      _ <- localVerify(tokenPurposedClaim)
      groupsCheck <- stateVerifier.verifyGroupsTokenPurposedClaim(tokenPurposedClaim)
      _ <- earlyResponseIf(!groupsCheck)(InvalidClaimException("Invalid Groups Create", "Groups couldn't be validated"))

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
      groupsCheck <- stateVerifier.verifyGroupsForVerificationRequest(verificationRequest, tokenPurposedClaim)

      _ <- earlyResponseIf(!groupsCheck)(InvalidClaimException("Invalid Groups Verify", "Groups couldn't be validated"))

    } yield {
      true
    }
  }

  override def processBootstrapToken(bootstrapRequest: BootstrapRequest): Task[BootstrapToken] = {

    def createClaim(scope: Scope, maybeIdentity: Option[UUID]): Task[TokenClaim] = for {
      tokenPurposedClaim <- buildTokenClaimFromUbirchTokenAsString(bootstrapRequest.token)
      tokenClaim = tokenPurposedClaim.copy(
        targetIdentities = maybeIdentity.map(x => Left(List(x))).getOrElse(Right(List("*"))),
        scopes = List(asString(scope))
      ).toTokenClaim(ENV)
    } yield tokenClaim

    for {
      isValid <- stateVerifier.verifyIdentitySignature(bootstrapRequest.identity, bootstrapRequest.signedRaw, bootstrapRequest.signatureRaw)
        .onErrorRecover { case e: Exception => throw InvalidClaimException("Invalid Key Signature Internal", e.getMessage) }
      _ <- earlyResponseIf(!isValid)(InvalidClaimException("Invalid Key Signature", "Invalid key"))

      thingCreate <- createClaim(Scope.Thing_Create, Some(bootstrapRequest.identity))
        .map(_.copy(expiration = Some(60 * 15)))
        .flatMap(create(_, 'purposed_claim))

      thingAnchor <- createClaim(Scope.UPP_Anchor, Some(bootstrapRequest.identity))
        .map(_.copy(expiration = None))
        .flatMap(create(_, 'purposed_claim))

      thingVerify <- createClaim(Scope.UPP_Verify, None)
        .map(_.copy(expiration = None))
        .flatMap(create(_, 'purposed_claim))

      thingDataStore <- createClaim(Scope.Thing_StoreData, Some(bootstrapRequest.identity))
        .map(_.copy(expiration = None))
        .flatMap(create(_, 'purposed_claim))

      _ = logger.debug("bootstrap_tokens_created create:{} anchor:{} verify:{} data:{}", thingCreate.token, thingAnchor.token, thingVerify.token, thingDataStore)

    } yield {
      BootstrapToken(thingCreate, thingAnchor, thingVerify, thingDataStore)
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
    (for {
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
    }).onErrorHandleWith {
      case e: JwtValidationException =>
        logger.error("error_building_token_claim=" + e.getMessage, e)
        Task.raiseError(InvalidClaimException("error_building_token_claim", e.getMessage))
    }
  }

  private def localVerify(tokenPurposedClaim: TokenPurposedClaim): Task[Boolean] = for {
    _ <- earlyResponseIf(!tokenPurposedClaim.validatePurpose)(InvalidClaimException("Invalid Purpose", "Purpose is not correct."))
    _ <- earlyResponseIf(!tokenPurposedClaim.hasMaybeGroups && !tokenPurposedClaim.hasMaybeIdentities && !tokenPurposedClaim.isIdentitiesWildcard)(InvalidClaimException("Invalid Target Identities and/or Groups", "Either have identities and/or groups"))
    _ <- earlyResponseIf(tokenPurposedClaim.hasMaybeIdentities && !tokenPurposedClaim.validateIdentities)(InvalidClaimException("Invalid Target Identities", "Target Identities are empty or invalid"))
    _ <- earlyResponseIf(tokenPurposedClaim.hasMaybeGroups && !tokenPurposedClaim.validateGroups)(InvalidClaimException("Invalid Target Groups", "Target Groups are empty or invalid"))
    _ <- earlyResponseIf(!tokenPurposedClaim.validateOriginsDomains)(InvalidClaimException("Invalid Origin Domains", "Origin Domains are empty or invalid"))
    _ <- earlyResponseIf(!tokenPurposedClaim.validateScopes)(InvalidClaimException(s"Invalid Scopes", s"Scopes are empty or invalid, current=${tokenPurposedClaim.scopes}"))
  } yield true

}
