package com.ubirch.services.state

import java.security.SignatureException
import java.util.UUID

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.ConfPaths.{ ExternalStateGetterPaths, GenericConfPaths }
import com.ubirch.{ StateVerifierException, TokenEncodingException }
import com.ubirch.models.{ Group, Key, Scope, TokenPurposedClaim, VerificationRequest }
import com.ubirch.services.config.ConfigProvider
import com.ubirch.services.execution.{ ExecutionProvider, SchedulerProvider }
import com.ubirch.services.formats.{ DefaultJsonConverterService, JsonConverterService, JsonFormatsProvider }
import com.ubirch.services.jwt.{ DefaultTokenEncodingService, DefaultTokenKeyService, TokenEncodingService, TokenKeyService }
import com.ubirch.services.lifeCycle.DefaultLifecycle
import com.ubirch.util.{ PublicKeyUtil, TaskHelpers }
import monix.eval.Task
import monix.execution.Scheduler
import net.logstash.logback.argument.StructuredArguments.v
import javax.inject.{ Inject, Singleton }

trait StateVerifier {
  def verifyGroupsTokenPurposedClaim(tokenPurposedClaim: TokenPurposedClaim): Task[Boolean]
  def verifyGroupsForVerificationRequest(verificationRequest: VerificationRequest, tokenPurposedClaim: TokenPurposedClaim): Task[Boolean]
  def verifyGroups(tokenPurposedClaim: TokenPurposedClaim, currentGroups: List[Group]): Boolean
  def verifyIdentitySignature(identityUUID: UUID, signed: Array[Byte], signature: Array[Byte]): Task[Boolean]

  def identityGroups(identityUUID: UUID): Task[List[Group]]
  def tenantGroups(tenantId: UUID): Task[List[Group]]
  def identityKey(identityUUID: UUID): Task[Option[Key]]
}

@Singleton
class DefaultStateVerifier @Inject() (
    config: Config,
    externalStateGetter: ExternalStateGetter,
    tokenKey: TokenKeyService,
    tokenEncodingService: TokenEncodingService,
    jsonConverterService: JsonConverterService,
    keyGetter: KeyGetter
)
  extends StateVerifier with TaskHelpers with LazyLogging {

  private final val ENV = config.getString(GenericConfPaths.ENV)
  private final val REALM_NAME: String = config.getString(ExternalStateGetterPaths.REALM_NAME)
  private final val STRICT_GROUP_CHECK: Boolean = config.getBoolean(ExternalStateGetterPaths.STRICT_GROUP_CHECK)

  override def verifyGroupsTokenPurposedClaim(tokenPurposedClaim: TokenPurposedClaim): Task[Boolean] = {
    if (tokenPurposedClaim.hasMaybeGroups) {
      tenantGroups(tokenPurposedClaim.tenantId)
        .map { gs => verifyGroups(tokenPurposedClaim, gs) }
    } else Task.delay(true)
  }

  override def verifyGroupsForVerificationRequest(verificationRequest: VerificationRequest, tokenPurposedClaim: TokenPurposedClaim): Task[Boolean] = {
    if (tokenPurposedClaim.hasMaybeGroups) {
      identityGroups(verificationRequest.identity)
        .map { gs => verifyGroups(tokenPurposedClaim, gs) }
    } else Task.delay(true)
  }

  override def verifyGroups(tokenPurposedClaim: TokenPurposedClaim, currentGroups: List[Group]): Boolean = {
    val check = tokenPurposedClaim.targetGroups match {
      case Right(claimedGroupAsNames) if currentGroups.nonEmpty && STRICT_GROUP_CHECK =>
        currentGroups.map(_.name).intersect(claimedGroupAsNames).sorted == claimedGroupAsNames.sorted
      case Left(claimedGroupAsUUIDs) if currentGroups.nonEmpty && STRICT_GROUP_CHECK =>
        currentGroups.map(_.id).intersect(claimedGroupAsUUIDs).sorted == claimedGroupAsUUIDs.sorted
      case Right(claimedGroupAsNames) if currentGroups.nonEmpty && !STRICT_GROUP_CHECK =>
        currentGroups.map(_.name).intersect(claimedGroupAsNames).sorted.nonEmpty
      case Left(claimedGroupAsUUIDs) if currentGroups.nonEmpty && !STRICT_GROUP_CHECK =>
        currentGroups.map(_.id).intersect(claimedGroupAsUUIDs).sorted.nonEmpty
      case _ => false
    }
    if (!check) {
      logger.warn("target_groups=" + tokenPurposedClaim.targetGroups.left.map(_.map(_.toString)).merge + " current_groups=" + currentGroups.toString())
    }
    check
  }

  override def verifyIdentitySignature(identityUUID: UUID, signed: Array[Byte], signature: Array[Byte]): Task[Boolean] = {
    lazy val taskKey = identityKey(identityUUID)

    def check(raw: Boolean): Task[Boolean] = taskKey.flatMap {
      case Some(key) =>
        for {
          pubKey <- Task.fromTry(key.getPubKey)
        } yield {
          //We want to check if maybe there is asn1
          pubKey.getPublicKey.getAlgorithm match {
            case "ECC_ECDSA" | "ecdsa-p256v1" | "ECDSA" if raw => pubKey.setSignatureAlgorithm("SHA256WITHPLAIN-ECDSA")
            case _ => ()
          }
          PublicKeyUtil.verifySHA512(pubKey, signed, signature)
        }
      case None => Task.delay(false)
    }

    check(raw = true).onErrorRecoverWith {
      case _: SignatureException =>
        logger.info("Trying to verify with possible ans1 signature")
        check(raw = false)
    }

  }

  override def identityGroups(identityUUID: UUID): Task[List[Group]] = {
    for {

      _ <- Task.unit

      tokenClaim = TokenPurposedClaim(
        tenantId = identityUUID, // What UUID to use here?
        purpose = "systems_interchange",
        targetIdentities = Left(List(identityUUID)),
        targetGroups = Left(Nil),
        expiration = Some(60 * 5),
        notBefore = None,
        originDomains = Nil,
        scopes = List(Scope.asString(Scope.Thing_GetInfo))
      ).toTokenClaim(ENV)
        .addContent(
          'realm_access -> "DEVICE",
          'realm_name -> REALM_NAME
        )

      res <- liftTry(tokenEncodingService.encode(UUID.randomUUID(), tokenClaim, tokenKey.key))(TokenEncodingException("Error creating token", tokenClaim))
      (token, _) = res

      res <- externalStateGetter.getDeviceGroups(token)
      resBody <- Task(new String(res.body))
      _ = logger.info(
        "deviceId:" + identityUUID.toString +
          " res_status:" + res.status +
          " res_body:" + resBody,
        v("deviceId", identityUUID)
      )

      groups <- Task.fromEither(jsonConverterService.as[List[Group]](resBody))
        .onErrorRecoverWith {
          case e: Exception =>
            logger.error("error_getting_device_groups=" + e.getMessage)
            Task.raiseError(StateVerifierException("Invalid Get Device Groups Response"))
        }

    } yield {
      groups
    }
  }

  override def tenantGroups(tenantId: UUID): Task[List[Group]] = {
    for {

      _ <- Task.unit

      tokenClaim = TokenPurposedClaim(
        tenantId = tenantId, // What UUID to use here?
        purpose = "systems_interchange",
        targetIdentities = Left(Nil),
        targetGroups = Left(Nil),
        expiration = Some(60 * 5),
        notBefore = None,
        originDomains = Nil,
        scopes = List(Scope.asString(Scope.User_GetInfo))
      ).toTokenClaim(ENV)
        .addContent(
          'realm_access -> "USER",
          'realm_name -> REALM_NAME
        )

      res <- liftTry(tokenEncodingService.encode(UUID.randomUUID(), tokenClaim, tokenKey.key))(TokenEncodingException("Error creating token", tokenClaim))
      (token, _) = res

      res <- externalStateGetter.getUserGroups(token)
      resBody <- Task(new String(res.body))
      _ = logger.info(
        "ownerId:" + tenantId.toString +
          " res_status:" + res.status +
          " res_body:" + resBody,
        v("ownerId", tenantId)
      )

      groups <- Task.fromEither(jsonConverterService.as[List[Group]](resBody))
        .onErrorRecoverWith {
          case e: Exception =>
            logger.error("error_getting_user_groups=" + e.getMessage)
            Task.raiseError(StateVerifierException("Invalid Get User Groups Response"))
        }

    } yield {
      groups
    }
  }

  override def identityKey(identityUUID: UUID): Task[Option[Key]] = {

    for {
      res <- keyGetter.byIdentityId(identityUUID)
      resBody <- Task(new String(res.body))

      _ = logger.info(
        "identityId:" + identityUUID.toString +
          " res_status:" + res.status +
          " res_body:" + resBody,
        v("identityId", identityUUID)
      )

      key <- Task.delay(jsonConverterService.fromJsonInput[List[Key]](resBody)(_ \ "pubKeyInfo").headOption)

    } yield {
      key
    }

  }
}

object DefaultStateVerifier extends {

  import scala.concurrent.Await
  import scala.concurrent.duration._
  import scala.language.postfixOps

  def await[T](task: Task[T], atMost: Duration)(implicit scheduler: Scheduler): T = {
    val future = task.runToFuture
    Await.result(future, atMost)
  }

  def main(args: Array[String]): Unit = {

    val config = new ConfigProvider get ()
    val e = new ExecutionProvider(config) get ()
    implicit val scheduler = new SchedulerProvider(e) get ()

    val lifecycle = new DefaultLifecycle()
    val client = new DefaultHttpClient(lifecycle)
    val externalStateGetter = new DefaultExternalGetter(config, client)
    val tokenKeyService = new DefaultTokenKeyService(config)
    val tokenEncodingService = new DefaultTokenEncodingService()

    val formats = new JsonFormatsProvider() get ()
    val jsonConverterService = new DefaultJsonConverterService()(formats)

    val keyGetter = new DefaultKeyGetter(config, client)

    val stateVerifier: StateVerifier = new DefaultStateVerifier(config, externalStateGetter, tokenKeyService, tokenEncodingService, jsonConverterService, keyGetter)

    //2021-07-14 19:36:53.851 INFO  [DefaultStateVerifier] deviceId:bdab47d0-fcf9-429e-a118-3dae0773cac2 res_status:200 res_body:[{"id":"d6e525c0-41e2-4a77-925c-4d6ea4fb8431","name":"Kitchen_Carlos"},{"id":"9192e299-333a-43b6-ad05-bcfae5ebdc10","name":"OWN_DEVICES_carlos.sanchez@ubirch.com"}]
    //List(
    // Group(d6e525c0-41e2-4a77-925c-4d6ea4fb8431,Kitchen_Carlos),
    // Group(9192e299-333a-43b6-ad05-bcfae5ebdc10,OWN_DEVICES_carlos.sanchez@ubirch.com))
    //val res = await(stateVerifier.identityGroups(UUID.fromString("bdab47d0-fcf9-429e-a118-3dae0773cac2")), 5 seconds)

    //2021-07-14 19:34:29.873 INFO  [DefaultStateVerifier] ownerId:963995ed-ce12-4ea5-89dc-b181701d1d7b res_status:200 res_body:[{"id":"d6e525c0-41e2-4a77-925c-4d6ea4fb8431","name":"Kitchen_Carlos"},{"id":"9192e299-333a-43b6-ad05-bcfae5ebdc10","name":"OWN_DEVICES_carlos.sanchez@ubirch.com"},{"id":"eda5bf51-ad00-49fa-8281-a0996893c1db","name":"ubirch_admins"}]
    //List(
    // Group(d6e525c0-41e2-4a77-925c-4d6ea4fb8431,Kitchen_Carlos),
    // Group(9192e299-333a-43b6-ad05-bcfae5ebdc10,OWN_DEVICES_carlos.sanchez@ubirch.com),
    // Group(eda5bf51-ad00-49fa-8281-a0996893c1db,ubirch_admins))
    //val res = await(stateVerifier.tenantGroups(UUID.fromString("963995ed-ce12-4ea5-89dc-b181701d1d7b")), 5 seconds)

    //val res = await(stateVerifier.identityKey(UUID.fromString("9011a2de-8c69-45be-bc47-60fd58e121ce")), 5 seconds)

    val res = await(stateVerifier.verifyGroupsForVerificationRequest(
      VerificationRequest("a token", UUID.fromString("bdab47d0-fcf9-429e-a118-3dae0773cac2"), None, None, None),
      TokenPurposedClaim(
        tenantId = null,
        purpose = "",
        targetIdentities = null,
        targetGroups = Left(List(UUID.fromString("d6e525c0-41e2-4a77-925c-4d6ea4fb8431"), UUID.fromString("d6e525c0-41e2-4a77-925c-3dae0773cac2"))),
        expiration = null,
        notBefore = null,
        originDomains = null,
        scopes = null
      )
    ), 5 seconds)

    println(res)

  }

}
