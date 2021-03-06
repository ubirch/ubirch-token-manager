package com.ubirch

import com.google.inject.binder.ScopedBindingBuilder
import com.google.inject.{ AbstractModule, Module }
import com.typesafe.config.Config
import com.ubirch.services.cluster._
import com.ubirch.services.config.ConfigProvider
import com.ubirch.services.execution.{ ExecutionProvider, SchedulerProvider }
import com.ubirch.services.formats.{ DefaultJsonConverterService, JsonConverterService, JsonFormatsProvider }
import com.ubirch.services.jwt._
import com.ubirch.services.key.{ DefaultHMAC, DefaultHMACVerifier, DefaultKeyPoolService, HMAC, HMACVerifier, KeyPoolService }
import com.ubirch.services.lifeCycle.{ DefaultJVMHook, DefaultLifecycle, JVMHook, Lifecycle }
import com.ubirch.services.rest.SwaggerProvider
import com.ubirch.services.state.{ DefaultExternalGetter, DefaultHttpClient, DefaultKeyGetter, DefaultSecretKeyPoolService, DefaultStateVerifier, DefaultTokenClientsInfo, ExternalStateGetter, HttpClient, KeyGetter, SecretKeyPoolService, StateVerifier, TokenClientsInfo }
import monix.execution.Scheduler
import org.json4s.Formats
import org.scalatra.swagger.Swagger

import scala.concurrent.ExecutionContext

/**
  * Represents the default binder for the system components
  */
class Binder
  extends AbstractModule {

  def Config: ScopedBindingBuilder = bind(classOf[Config]).toProvider(classOf[ConfigProvider])
  def ExecutionContext: ScopedBindingBuilder = bind(classOf[ExecutionContext]).toProvider(classOf[ExecutionProvider])
  def Scheduler: ScopedBindingBuilder = bind(classOf[Scheduler]).toProvider(classOf[SchedulerProvider])
  def Swagger: ScopedBindingBuilder = bind(classOf[Swagger]).toProvider(classOf[SwaggerProvider])
  def Formats: ScopedBindingBuilder = bind(classOf[Formats]).toProvider(classOf[JsonFormatsProvider])
  def Lifecycle: ScopedBindingBuilder = bind(classOf[Lifecycle]).to(classOf[DefaultLifecycle])
  def JVMHook: ScopedBindingBuilder = bind(classOf[JVMHook]).to(classOf[DefaultJVMHook])
  def JsonConverterService: ScopedBindingBuilder = bind(classOf[JsonConverterService]).to(classOf[DefaultJsonConverterService])
  def ClusterService: ScopedBindingBuilder = bind(classOf[ClusterService]).to(classOf[DefaultClusterService])
  def ConnectionService: ScopedBindingBuilder = bind(classOf[ConnectionService]).to(classOf[DefaultConnectionService])
  def TokenService: ScopedBindingBuilder = bind(classOf[TokenService]).to(classOf[DefaultTokenService])
  def TokenEncodingService: ScopedBindingBuilder = bind(classOf[TokenEncodingService]).to(classOf[DefaultTokenEncodingService])
  def TokenDecodingService: ScopedBindingBuilder = bind(classOf[TokenDecodingService]).to(classOf[DefaultTokenDecodingService])
  def KeyPoolService: ScopedBindingBuilder = bind(classOf[KeyPoolService]).to(classOf[DefaultKeyPoolService])
  def PublicKeyDiscoveryService: ScopedBindingBuilder = bind(classOf[PublicKeyDiscoveryService]).to(classOf[DefaultPublicKeyDiscoveryService])
  def PublicKeyPoolService: ScopedBindingBuilder = bind(classOf[PublicKeyPoolService]).to(classOf[DefaultPublicKeyPoolService])
  def TokenKeyService: ScopedBindingBuilder = bind(classOf[TokenKeyService]).to(classOf[DefaultTokenKeyService])
  def ExternalStateGetter: ScopedBindingBuilder = bind(classOf[ExternalStateGetter]).to(classOf[DefaultExternalGetter])
  def StateVerifier: ScopedBindingBuilder = bind(classOf[StateVerifier]).to(classOf[DefaultStateVerifier])
  def TokenClientsInfo: ScopedBindingBuilder = bind(classOf[TokenClientsInfo]).to(classOf[DefaultTokenClientsInfo])
  def SecretKeyPoolService: ScopedBindingBuilder = bind(classOf[SecretKeyPoolService]).to(classOf[DefaultSecretKeyPoolService])
  def HMAC: ScopedBindingBuilder = bind(classOf[HMAC]).to(classOf[DefaultHMAC])
  def HMACVerifier: ScopedBindingBuilder = bind(classOf[HMACVerifier]).to(classOf[DefaultHMACVerifier])
  def HttpClient: ScopedBindingBuilder = bind(classOf[HttpClient]).to(classOf[DefaultHttpClient])
  def KeyGetter: ScopedBindingBuilder = bind(classOf[KeyGetter]).to(classOf[DefaultKeyGetter])

  def configure(): Unit = {
    Config
    ExecutionContext
    Scheduler
    Swagger
    Formats
    Lifecycle
    JVMHook
    JsonConverterService
    ClusterService
    ConnectionService
    TokenService
    TokenEncodingService
    TokenDecodingService
    KeyPoolService
    PublicKeyDiscoveryService
    PublicKeyPoolService
    TokenKeyService
    ExternalStateGetter
    StateVerifier
    TokenClientsInfo
    SecretKeyPoolService
    HMAC
    HMACVerifier
    HttpClient
    KeyGetter
    ()
  }

}

object Binder {
  def modules: List[Module] = List(new Binder)
}
