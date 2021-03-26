package com.ubirch.v1

import com.google.inject.binder.ScopedBindingBuilder
import com.google.inject.{ AbstractModule, Module }
import com.typesafe.config.Config
import com.ubirch.v1.services.cluster.{ ClusterService, ConnectionService, DefaultClusterService, DefaultConnectionService }
import com.ubirch.v1.services.config.ConfigProvider
import com.ubirch.v1.services.execution.{ ExecutionProvider, SchedulerProvider }
import com.ubirch.v1.services.formats.{ DefaultJsonConverterService, JsonConverterService, JsonFormatsProvider }
import com.ubirch.v1.services.jwt._
import com.ubirch.v1.services.lifeCycle.{ DefaultJVMHook, DefaultLifecycle, JVMHook, Lifecycle }
import com.ubirch.v1.services.rest.SwaggerProvider
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
  def TokenStoreService: ScopedBindingBuilder = bind(classOf[TokenStoreService]).to(classOf[DefaultTokenStoreService])
  def TokenCreationService: ScopedBindingBuilder = bind(classOf[TokenCreationService]).to(classOf[DefaultTokenCreationService])
  def TokenVerificationService: ScopedBindingBuilder = bind(classOf[TokenVerificationService]).to(classOf[DefaultTokenVerificationService])
  def PublicKeyDiscoveryService: ScopedBindingBuilder = bind(classOf[PublicKeyDiscoveryService]).to(classOf[DefaultPublicKeyDiscoveryService])
  def PublicKeyPoolService: ScopedBindingBuilder = bind(classOf[PublicKeyPoolService]).to(classOf[DefaultPublicKeyPoolService])
  def TokenKeyService: ScopedBindingBuilder = bind(classOf[TokenKeyService]).to(classOf[DefaultTokenKeyService])

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
    TokenStoreService
    TokenCreationService
    TokenVerificationService
    PublicKeyDiscoveryService
    PublicKeyPoolService
    TokenKeyService
    ()
  }

}

object Binder {
  def modules: List[Module] = List(new Binder)
}
